package SimpleFactoryPlayer.Implementation;


import AgentSystemPluginAPI.Contract.IStateActionGenerator;
import AgentSystemPluginAPI.Contract.StateAction;
import Factory.GameLogic.Enums.Direction;
import SimpleFactoryPlayer.Implementation.Entities.RawField;
import SimpleFactoryPlayer.Implementation.Entities.RawState;
import SimpleFactoryPlayer.Implementation.Enums.FieldType;
import SimpleFactoryPlayer.Implementation.Enums.FriendFoe;
import SimpleFactoryPlayer.Implementation.Enums.SpawnInterval;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: TwiG
 * Date: 04.06.12
 * Time: 20:55
 * To change this template use File | Settings | File Templates.
 */
public class StateActionGenerator implements IStateActionGenerator {
// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface IStateActionGenerator ---------------------

    @Override
    public Set<StateAction> getAllPossibleActions(StateAction stateAction) {
        String encryptedState = stateAction.getStateDescription();
        RawState state = decryptState(encryptedState);
        List<RawField> rawFieldList = state.getFieldListRepresentation();
        Set<StateAction> stateActionSet = new HashSet<StateAction>();
        List<Integer> deleteList = new ArrayList<Integer>();

        stateActionSet.add(new StateAction(encryptedState, Integer.toString(0)));
        stateActionSet.add(new StateAction(encryptedState, Integer.toString(1)));
        stateActionSet.add(new StateAction(encryptedState, Integer.toString(2)));
        stateActionSet.add(new StateAction(encryptedState, Integer.toString(3)));
        stateActionSet.add(new StateAction(encryptedState, Integer.toString(4)));
        stateActionSet.add(new StateAction(encryptedState, Integer.toString(5)));
        stateActionSet.add(new StateAction(encryptedState, Integer.toString(6)));
        stateActionSet.add(new StateAction(encryptedState, Integer.toString(7)));
        stateActionSet.add(new StateAction(encryptedState, Integer.toString(8)));


        if (!(state.getMiddle().getFieldType() == FieldType.FACTORY)) {
            for (int i = 1; i < rawFieldList.size(); i++) {
                if (rawFieldList.get(i).getFieldType() == FieldType.FACTORY) {
                    deleteList.add(i);
                }
            }
        }

        for (Integer del : deleteList) {
            stateActionSet.remove(new StateAction(encryptedState, del.toString()));
        }
        deleteList = new ArrayList<Integer>();


        for (int i = 1; i < rawFieldList.size(); i++) {
            if (rawFieldList.get(i).getUnit() == FriendFoe.FRIEND || rawFieldList.get(i).getUnit() == FriendFoe.EXHAUSTED_FRIEND) {
                deleteList.add(i);
            }
        }

        for (Integer del : deleteList) {
            stateActionSet.remove(new StateAction(encryptedState, del.toString()));
        }

        return stateActionSet;
    }

// -------------------------- PUBLIC METHODS --------------------------

    public RawState decryptState(String encryptedState) {
        byte[] bytes;

        bytes = encryptedState.getBytes(Charset.forName("UTF-8"));

        //System.out.println(String.format("String after getBytes: %h",bytes));
        RawState rawState = new RawState();
        rawState.setSignal(decryptDirection(Byte.toString(bytes[9])));
        List<RawField> rawFieldList = new ArrayList<RawField>();

        //9 times
        for (int i = 0; i < 9; i++) {
            //System.out.println(String.format(i+" %h",bytes[i]));
            rawFieldList.add(decryptField(bytes[i]));
        }

        //System.out.println(rawFieldList);

        rawState.setFieldListRepresentation(rawFieldList);

        return rawState;
    }

    public Direction decryptDirection(String actionDescription) {
        byte directionByte = Byte.parseByte(actionDescription);
        Direction direction = null;
        switch (directionByte) {
            case 0:
                direction = null;
                break;
            case 1:
                direction = Direction.UP;
                break;
            case 2:
                direction = Direction.UP_RIGHT;
                break;
            case 3:
                direction = Direction.RIGHT;
                break;
            case 4:
                direction = Direction.DOWN_RIGHT;
                break;
            case 5:
                direction = Direction.DOWN;
                break;
            case 6:
                direction = Direction.DOWN_LEFT;
                break;
            case 7:
                direction = Direction.LEFT;
                break;
            case 8:
                direction = Direction.UP_LEFT;
                break;
        }
        return direction;
    }

    public RawField decryptField(Byte encryptedField) {
        RawField rawField = new RawField();
        int encrypted = encryptedField;

        //System.out.println(String.format(" %h",encrypted));
        //encrypted = encryptedField ^ 0xFFFFFFFF;
        //encrypted++;
        //System.out.println(String.format(" %h",encrypted));


        int a12 = encryptedField & 0x00000003;
        int a34 = (encryptedField >>> 2) & 0x00000003;
        int a56 = (encryptedField >>> 4) & 0x00000003;
        int a78 = (encryptedField >>> 6) & 0x00000003;
        //Remaining Time to Spawn will not be decrypted


        rawField.setFieldController(FriendFoe.get(a12));
        rawField.setFieldType(FieldType.get(a34));
        rawField.setUnit(FriendFoe.get(a56));


        return rawField;
    }

    public String encryptState(RawState rawState) {
        // 1 Slot[4] = ( von high nach least)  FeldTyp[1] UnitTyp[3]
        // 9 slots[36] + Direction[2]
        // --> 5 Byte
        // Aber noch probleme beim decrypten! Da nicht genügend infos zum Legale richtung finden da sind!

        //Verschwenderische version 1 Byte pro feld + direction 10 Byte
        //
        // Unit[2](None,friend,enemy,exhaustedFriend)
        // FieldType[2](normal,InfluenceActive,InfluencePassive,Factory)    //aktive felder ansatz vorerst verworfen
        // FieldController[2](Neutral,red,blue) 1/2 bit über
        // RemainingTimetoSpawn[2] -> 4 intervalle

        // String mit Byte array füttern

        byte[] encryptedState = new byte[10];

        List<RawField> fieldList = rawState.getFieldListRepresentation();

        for (int i = 0; i < fieldList.size(); i++) {
            encryptedState[i] = getEncryptedByte(fieldList.get(i));
        }
        Direction direction = rawState.getSignal();
        if (direction == null) {
            encryptedState[9] = 0;
        } else if (direction == Direction.UP) {
            encryptedState[9] = 1;
        } else if (direction == Direction.RIGHT) {
            encryptedState[9] = 3;
        } else if (direction == Direction.DOWN) {
            encryptedState[9] = 5;
        } else if (direction == Direction.LEFT) {
            encryptedState[9] = 7;
        }

        /*
        for(int i=0; i<fieldList.size();i++){
            System.out.print(String.format("[" + i + "]%h", encryptedState[i]));

        }
        */
        //System.out.println(String.format("String as string %h", encryptedState));

        return new String(encryptedState);
    }

    public Byte getEncryptedByte(RawField field) {
        byte encryption = 0;

        int a12 = field.getFieldController().ordinal();
        int a34 = field.getFieldType().ordinal();
        int a56 = field.getUnit().ordinal();
        int a78 = SpawnInterval.LONG.ordinal();
        int tts = field.getRemainingTimeToSpawn();
        if (tts < 2) {
            a78 = SpawnInterval.VERY_SHORT.ordinal();
        }
        /*
        else if(tts <3){
            a78 = SpawnInterval.SHORT.ordinal();
        }
        else if(tts <4){
            a78 = SpawnInterval.MIDDLE.ordinal();
        }
        */
        a34 = a34 << 2;
        a56 = a56 << 4;
        a78 = a78 << 6;
        int beforeEncrypt = a12 | a34 | a56 | a78;
        encryption = (byte) beforeEncrypt;

        //b |= (1 << bitIndex); // set a bit to 1
        //b &= ~(1 << bitIndex); // set a bit to 0

        //System.out.println(String.format("String Single byte: %h",encryption));

        return encryption;
    }
}
