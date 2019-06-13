package com.photon.photonchain.network.utils;

import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.proto.InesvMessage;
import com.photon.photonchain.network.proto.MessageManager;
import com.photon.photonchain.storage.constants.Constants;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.photon.photonchain.network.proto.EventTypeEnum.EventType.SET_ZERO_PARTICIPANT;
import static com.photon.photonchain.network.proto.MessageTypeEnum.MessageType.RESPONSE;

/**
 * @author Wu Created by SKINK on 2018/3/1.
 */

@Component
public class FoundryUtils {
    @Autowired
    private FoundryMachineManager foundryMachineManager;
    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;
    @Autowired
    private SyncParticipantManager syncParticipantManager;
    @Autowired
    private AssetsManager assetsManager;

    public boolean resetParticipant = false;

    private static org.slf4j.Logger logger = LoggerFactory.getLogger ( FoundryUtils.class );

    public static int getDiffYear(long genesis, long current) {
        Calendar calendarOne = Calendar.getInstance ( );
        calendarOne.setTimeInMillis ( genesis );
        Calendar calendarTwo = Calendar.getInstance ( );
        calendarTwo.setTimeInMillis ( current );
        int year1 = calendarOne.get ( Calendar.YEAR );
        int year2 = calendarTwo.get ( Calendar.YEAR );
        return (year2 - year1);
    }


    public static long getBlockReward(byte[] publicKey, int diffYear, Long blockHeight, InitializationManager initializationManager, boolean ignoreUnverified) {
        //全预挖
        return 0L;
    }


    /**
     * Map排序
     */
    public static Map<String, Integer> getSortingMap(Map<String, Integer> map) {

        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>> ( map.entrySet ( ) );

        Collections.sort ( list, new Comparator<Map.Entry<String, Integer>> ( ) {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return o1.getValue ( ).compareTo ( o2.getValue ( ) );
            }
        } );

        Map<String, Integer> newMap = new LinkedHashMap<> ( );
        for (Map.Entry<String, Integer> mapping : list) {
            newMap.put ( mapping.getKey ( ), mapping.getValue ( ) );
        }
        return newMap;
    }

    public synchronized void resetParticipant() {
        long time = DateUtil.getWebTime ( );
        long resetParticipantTime = foundryMachineManager.getResetParticipantTime ( );
        if ( time - resetParticipantTime > 10000 ) {
            logger.info ( "resetParticipant" );
            Map<String, Integer> participantList = syncParticipantManager.getParticipantList ( );
            for (String pubKey : participantList.keySet ( )) {
                Map<String, Long> fromAssetsPtn = assetsManager.getAccountAssets ( pubKey, Constants.PTN );
                syncParticipantManager.setParticipant ( pubKey, foundryMachineManager.getFoundryMachineCount ( fromAssetsPtn ) );
            }
            foundryMachineManager.setWaitfoundryMachine ( getFoundryMachiner ( ) );
            foundryMachineManager.setWaitFoundryMachineCount ( 1 );
            foundryMachineManager.setResetParticipantTime ( time );
            resetParticipant = true;
        }
    }

    public String getFoundryMachiner() {
        Map<String, Integer> participantList = FoundryUtils.getSortingMap ( syncParticipantManager.getParticipantList ( ) );
        String foundryMachiner = null;
        for (String pubKey : participantList.keySet ( )) {
            if ( foundryMachiner == null ) {
                foundryMachiner = pubKey;
                continue;
            }
            if ( participantList.get ( pubKey ) > participantList.get ( foundryMachiner ) ) {
                foundryMachiner = pubKey;
            }
        }
        if ( foundryMachiner != null && participantList.get ( foundryMachiner ) <= 0 ) {
            InesvMessage.Message.Builder builder = MessageManager.createMessageBuilder ( RESPONSE, SET_ZERO_PARTICIPANT );
            List<String> hostList = nioSocketChannelManager.getChannelHostList ( );
            builder.addAllNodeAddressList ( hostList );
            nioSocketChannelManager.write ( builder.build ( ) );
            return null;
        }
        return foundryMachiner;
    }
}
