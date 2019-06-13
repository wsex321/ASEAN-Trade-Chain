package com.photon.photonchain.network.core;

import com.photon.photonchain.network.ehcacheManager.*;
import com.photon.photonchain.network.excutor.TotalTranExcutor;
import com.photon.photonchain.network.proto.ParticipantMessage;
import com.photon.photonchain.network.utils.DateUtil;
import com.photon.photonchain.network.utils.FileUtil;
import com.photon.photonchain.network.utils.FoundryUtils;
import com.photon.photonchain.network.utils.NetWorkUtil;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.entity.Block;
import com.photon.photonchain.storage.entity.Token;
import com.photon.photonchain.storage.entity.Transaction;
import com.photon.photonchain.storage.entity.UnconfirmedTran;
import com.photon.photonchain.storage.repository.BlockRepository;
import com.photon.photonchain.storage.repository.TokenRepository;
import com.photon.photonchain.storage.repository.TransactionRepository;
import com.photon.photonchain.storage.utils.ContractUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

/**
 * @Author:Lin
 * @Description:
 * @Date:17:20 2018/6/19
 * @Modified by:
 */
@Component
public class SyncTimer {
    @Autowired
    private SyncBlockManager syncBlockManager;
    @Autowired
    private NioSocketChannelManager nioSocketChannelManager;
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private InitializationManager initializationManager;
    @Autowired
    private Verification verification;
    @Autowired
    private SyncBlock syncBlock;
    @Autowired
    private SyncUnconfirmedTran syncUnconfirmedTran;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private SyncUnconfirmedTranManager syncUnconfirmedTranManager;
    @Autowired
    private UnconfirmedTranManager unconfirmedTranManager;
    @Autowired
    private SyncToken syncToken;
    @Autowired
    private SyncParticipant syncParticipant;
    @Autowired
    private SyncTokenManager syncTokenManager;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private FoundryMachineManager foundryMachineManager;
    @Autowired
    private SyncParticipantManager syncParticipantManager;
    @Autowired
    private ResetData resetData;
    @Autowired
    private TotalTranExcutor totalTranExcutor;
    @Autowired
    private StatisticalAssetsManager statisticalAssetsManager;


    private Logger logger = LoggerFactory.getLogger ( SyncTimer.class );

    public boolean syncBlockTimer = false;

    public boolean syncTransactionTimer = false;

    public boolean syncTokenTimer = false;

    public boolean syncParticipantTimer = false;

    public boolean startSyncBlock = false;

    public boolean startSyncTransaction = false;

    public boolean startSyncToken = false;

    public boolean startSyncParticipant = false;

    public synchronized void SyncBlockTimer() {
        if ( !syncBlockTimer ) {
            syncBlockTimer = true;
            Timer timer = new Timer ( );
            timer.schedule ( new TimerTask ( ) {
                long startTime = System.currentTimeMillis ( );

                @Override
                public void run() {
                    int syncBlockResponseCount = syncBlockManager.getSyncQueue ( ).size ( );
                    long waitTime = System.currentTimeMillis ( ) - startTime;
                    if ( syncBlockResponseCount >= Constants.SYNC_WAIT_COUNT || syncBlockManager.getSyncCount ( ) == syncBlockResponseCount || syncBlockResponseCount == nioSocketChannelManager.getActiveNioSocketChannelCount ( ) || waitTime > Constants.SYNC_WAIT_TIME ) {
                        startSyncBlock = true;
                        timer.cancel ( );
                        boolean coincident = syncBlockManager.isCoincident ( );
                        if ( coincident ) {
                            for (int i = 0; i < syncBlockResponseCount; i++) {
                                Map queueMap = syncBlockManager.getSyncBlockQueue ( );
                                if ( queueMap == null ) {
                                    continue;
                                }
                                List<Block> syncBlockList = (List<Block>) queueMap.get ( Constants.SYNC_BLOCK_LIST );
                                if ( !syncBlockList.isEmpty ( ) ) {
                                    boolean verifyPrevHash = Arrays.equals ( syncBlockList.get ( 0 ).getBlockHead ( ).getHashPrevBlock ( ), Hex.decode ( initializationManager.getLastBlock ( ).getBlockHash ( ) ) );
                                    if ( verifyPrevHash ) {
                                        saveBlocks ( syncBlockList );
                                        blockRepository.save ( syncBlockList );
                                        initializationManager.setLastBlock ( syncBlockList.get ( syncBlockList.size ( ) - 1 ) );
                                    } else {
                                        syncBlockManager.setSyncBlock ( true );
                                        resetData.backBlock ( 100 );
                                        logger.info ( "可能处于分叉" );
                                    }
                                    break;
                                }
                            }
                        } else {
                            for (int i = 0; i < syncBlockResponseCount; i++) {
                                long blockHeight = initializationManager.getBlockHeight ( );
                                Map queueMap = syncBlockManager.getSyncBlockQueue ( );
                                if ( queueMap == null ) {
                                    continue;
                                }
                                long syncBlockHieght = (long) queueMap.get ( Constants.SYNC_BLOCK_HEIGHT );
                                List<Block> syncBlockList = (List<Block>) queueMap.get ( Constants.SYNC_BLOCK_LIST );
                                if ( blockHeight < syncBlockHieght ) {
                                    String macAddress = (String) queueMap.get ( Constants.SYNC_MAC_ADDRESS );
                                    String localMacAddress = NetWorkUtil.getMACAddress ( );
                                    if ( !localMacAddress.equals ( macAddress ) ) {
                                        if ( syncBlockList.size ( ) == 0 ) {
                                            nioSocketChannelManager.removeTheMac ( macAddress );
                                            break;
                                        }
                                    }
                                    List<Block> saveBlock = verification.verificationSyncBlockList ( syncBlockList, macAddress );
                                    if ( saveBlock.size ( ) > 0 ) {
                                        saveBlocks ( saveBlock );
                                        blockRepository.save ( saveBlock );
                                        initializationManager.setLastBlock ( saveBlock.get ( saveBlock.size ( ) - 1 ) );
                                    }
                                }
                            }
                        }
                        if ( syncBlockManager.needSyncBlockHeight ( ) > initializationManager.getBlockHeight ( ) ) {
                            syncBlock.init ( );
                        } else {
                            syncBlockManager.setSyncBlock ( false );
                            syncUnconfirmedTran.init ( );
                        }
                    }
                }
            }, 0, 1000 );

        }
    }

    public synchronized void SyncTransactionTimer() {
        if ( !syncTransactionTimer ) {
            syncTransactionTimer = true;
            Timer timer = new Timer ( );
            timer.schedule ( new TimerTask ( ) {
                long startTime = System.currentTimeMillis ( );

                @Override
                public void run() {
                    long waitTime = System.currentTimeMillis ( ) - startTime;
                    int syncTransactionResponseCount = syncUnconfirmedTranManager.getSyncQueue ( ).size ( );
                    if ( syncTransactionResponseCount >= Constants.SYNC_WAIT_COUNT || syncUnconfirmedTranManager.getSyncCount ( ) == syncTransactionResponseCount || syncTransactionResponseCount == nioSocketChannelManager.getActiveNioSocketChannelCount ( ) || waitTime > Constants.SYNC_WAIT_TIME ) {
                        startSyncTransaction = true;
                        timer.cancel ( );
                        for (int i = 0; i < syncTransactionResponseCount; i++) {
                            Map queueMap = syncUnconfirmedTranManager.getTransactionQueue ( );
                            if ( queueMap == null ) {
                                continue;
                            }
                            List<UnconfirmedTran> syncTransactionList = (List<UnconfirmedTran>) queueMap.get ( Constants.SYNC_TRANSACTION_LIST );
                            long blockHeight = (long) queueMap.get ( Constants.SYNC_BLOCK_HEIGHT );
                            if ( initializationManager.getBlockHeight ( ) == blockHeight ) {
                                List<UnconfirmedTran> saveTransaction = verification.verificationSyncUnconfirmedTranList ( syncTransactionList );
                                if ( saveTransaction.size ( ) > 0 ) {
                                    for (UnconfirmedTran unconfirmedTran : saveTransaction) {
                                        String key = Hex.toHexString ( unconfirmedTran.getTransSignature ( ) );
                                        if ( unconfirmedTran.getTransType ( ) == 5 || unconfirmedTran.getTransType ( ) == 6 ) {
                                            Transaction contractTrans = transactionRepository.findByContract ( unconfirmedTran.getContractAddress ( ), 3 );
                                            if ( contractTrans == null ) {
                                                continue;
                                            }
                                            if ( unconfirmedTran.getTransType ( ) == 5 && contractTrans.getContractState ( ) == 2 ) {
                                                continue;
                                            }
                                            if ( unconfirmedTran.getTransType ( ) == 6 && contractTrans.getContractState ( ) == 1 ) {
                                                continue;
                                            }
                                            //TODO hwh 并发兑换或取消
                                            key = unconfirmedTran.getContractAddress ( );
                                            UnconfirmedTran unconfirmedTranVerification = unconfirmedTranManager.getUnconfirmedTranMap ( ).get ( key );
                                            if ( unconfirmedTranVerification != null ) {
                                                String from = unconfirmedTranVerification.getTransFrom ( );
                                                if ( unconfirmedTran.getTransTo ( ).equals ( from ) ) {
                                                    key = key + "_" + Constants.EXCHANGE;
                                                } else {
                                                    continue;
                                                }
                                            }
                                        } else if ( unconfirmedTran.getTransType ( ) == 3 && unconfirmedTran.getContractType ( ) == 3 ) {
                                            key = unconfirmedTran.getTokenName ( );
                                        } else if ( unconfirmedTran.getTransType ( ) == 4 && unconfirmedTran.getContractType ( ) == 3 ) {
                                            Map<String, String> binMap = ContractUtil.analisysTokenContract ( unconfirmedTran.getContractBin ( ) );
                                            if ( binMap != null ) {
                                                key = binMap.get ( "tokenName" ) + "_4";
                                            }
                                        } else if ( unconfirmedTran.getTransType ( ) == 4 && unconfirmedTran.getContractType ( ) == 2 && !unconfirmedTran.getRemark ( ).equals ( "" ) ) {
                                            key = unconfirmedTran.getContractAddress ( ) + "_4" + "_" + unconfirmedTran.getTransFrom ( );
                                        } else if ( unconfirmedTran.getTransType ( ) == 4 ) {
                                            key = unconfirmedTran.getContractAddress ( ) + "_4";
                                        }
                                        unconfirmedTranManager.putUnconfirmedTran ( key, unconfirmedTran );
                                    }
                                    Set<String> pubkeySet = new HashSet<> ( );
                                    saveTransaction.forEach ( transaction -> {
                                        pubkeySet.add ( transaction.getTransFrom ( ) );
                                        pubkeySet.add ( transaction.getTransTo ( ) );
                                    } );
                                    initializationManager.saveAddressAndPubKey ( pubkeySet );
                                }
                                break;
                            }
                        }
                        syncUnconfirmedTranManager.setSyncTransaction ( false );
                        syncTokenManager.setSyncToken ( false );
                        syncParticipant.init ( );
                    }
                }
            }, 0, 1000 );
        }
    }

    public synchronized void syncTokenTimer() {
        if ( !syncTokenTimer ) {
            syncTokenTimer = true;
            Timer timer = new Timer ( );
            timer.schedule ( new TimerTask ( ) {
                long startTime = System.currentTimeMillis ( );

                @Override
                public void run() {
                    int syncTokenResponseCount = syncTokenManager.getSyncQueue ( ).size ( );
                    long waitTime = System.currentTimeMillis ( ) - startTime;
                    if ( syncTokenResponseCount >= Constants.SYNC_WAIT_COUNT || syncTokenManager.getSyncCount ( ) == syncTokenResponseCount || syncTokenResponseCount == nioSocketChannelManager.getActiveNioSocketChannelCount ( ) || waitTime > Constants.SYNC_WAIT_TIME ) {
                        startSyncToken = true;
                        timer.cancel ( );
                        for (int i = 0; i < syncTokenResponseCount; i++) {
                            List syncTokenList = syncTokenManager.getSyncTokenQueue ( );
                            if ( syncTokenList == null ) {
                                continue;
                            }
                            List<Token> saveTokenList = verification.verificationSyncTokenList ( syncTokenList );
                            if ( saveTokenList.size ( ) > 0 ) {
                                tokenRepository.save ( saveTokenList );
                                for (Token token : saveTokenList) {
                                    initializationManager.addTokenDecimal ( token.getName ( ), token.getDecimals ( ) );
                                }
                            }
                        }
                        syncTokenManager.setSyncToken ( false );
                        syncParticipant.init ( );
                    }
                }
            }, 0, 1000 );
        }
    }

    public synchronized void syncParticipantTimer() {
        if ( !syncParticipantTimer ) {
            syncParticipantTimer = true;
            Timer timer = new Timer ( );
            timer.schedule ( new TimerTask ( ) {
                long startTime = System.currentTimeMillis ( );

                @Override
                public void run() {
                    int syncParticipantResponseCount = syncParticipantManager.getSyncQueue ( ).size ( );
                    long waitTime = System.currentTimeMillis ( ) - startTime;
                    if ( syncParticipantResponseCount >= Constants.SYNC_WAIT_COUNT || syncParticipantManager.getSyncCount ( ) == syncParticipantResponseCount || syncParticipantResponseCount == nioSocketChannelManager.getActiveNioSocketChannelCount ( ) || waitTime > Constants.SYNC_WAIT_TIME ) {
                        startSyncParticipant = true;
                        timer.cancel ( );
                        boolean finish = false;
                        for (int i = 0; i < syncParticipantResponseCount; i++) {
                            Map mapQueue = syncParticipantManager.getParticipantQueue ( );
                            if ( mapQueue == null ) {
                                continue;
                            }
                            if ( initializationManager.getBlockHeight ( ) == (Long) mapQueue.get ( Constants.SYNC_BLOCK_HEIGHT ) ) {
                                List<ParticipantMessage.Participant> participants = (List<ParticipantMessage.Participant>) mapQueue.get ( Constants.SYNC_PARTICIPANT_LIST );
                                participants.forEach ( participant -> {
                                    syncParticipantManager.addParticipant ( participant.getParticipant ( ), participant.getCount ( ) );
                                } );
                                foundryMachineManager.setWaitFoundryMachineCount ( 1 );
                                logger.info ( "同步完成铸造机列表：" + FoundryUtils.getSortingMap ( syncParticipantManager.getParticipantList ( ) ) );
                                finish = true;
                                break;
                            }
                        }
                        if ( !finish ) {
                            syncBlockManager.setSyncBlock ( true );
                            resetData.backBlock ( 2 );
                            syncBlock.init ( );
                            return;
                        }
                        if ( syncUnconfirmedTranManager.getHasNewTransaction ( ) || syncBlockManager.getHasNewBlock ( ) || syncTokenManager.getHasNewToken ( ) ) {
                            syncBlockManager.setSyncBlock ( true );
                            resetData.backBlock ( 2 );
                            syncBlock.init ( );
                        } else {
                            syncParticipantManager.setSyncParticipant ( false );
                            logger.info ( "同步完成总资产：" + initializationManager.getTokenAssets ( Constants.PTN, false ) );
                            syncBlockManager.setSyncBlock ( false );
                            syncUnconfirmedTranManager.setSyncTransaction ( false );
                            syncTokenManager.setSyncToken ( false );
                            syncParticipantManager.setSyncParticipant ( false );

                        }
                    }
                }
            }, 0, 1000 );
        }
    }

    public void count() {
        Timer timer = new Timer ( );
        timer.schedule ( new TimerTask ( ) {
            @Override
            public void run() {
                countTranscNum ( );
            }
        }, 0, 1000 * 60 * 60 );
    }

    private void saveBlocks(List<Block> saveBlock) {
        Set<String> pubkeySet = new HashSet<> ( );
        saveBlock.forEach ( block -> {
            try {
                transactionRepository.save ( block.getBlockTransactions ( ) );
                block.getBlockTransactions ( ).forEach ( transaction -> {
                    //TODO:addressAndPubkey
                    if ( transaction.getTransType ( ) != 2 ) {
                        pubkeySet.add ( transaction.getTransFrom ( ) );
                        pubkeySet.add ( transaction.getTransTo ( ) );
                    }
                    if ( transaction.getTransType ( ) == 1 ) { //TODO:token
                        Map<String, String> binMap = ContractUtil.analisysTokenContract ( transaction.getContractBin ( ) );
                        if ( binMap != null ) {
                            Token token = new Token ( "", binMap.get ( "tokenName" ), "", Integer.valueOf ( binMap.get ( "tokenDecimal" ) ) );
                            tokenRepository.save ( token );
                            //token cache
                            initializationManager.addTokenDecimal ( token.getName ( ), token.getDecimals ( ) );
                        }
                    }
                } );
            } catch (Exception e) {
                e.printStackTrace ( );
            }
        } );
        initializationManager.saveAddressAndPubKey ( pubkeySet );
    }

    /**
     * 统计每小时交易数量
     */
    private void countTranscNum() {
        long start = System.currentTimeMillis ( );
        //先判断本地文件是否存在
        String path = "./photon_chain/" + "count.txt";
        //String path = System.getProperty("user.home") + "\\account\\" + "count.txt";
        File file = new File ( path );
        try {
            if ( !file.exists ( ) ) {
                List<Transaction> transactionList = transactionRepository.findAllASC ( );
                Transaction fristTransaction = transactionList.get ( 1 );
                Transaction lastTransaction = transactionList.get ( transactionList.size ( ) - 1 );//最后一个
                long blockHeight = 0;
                long time = fristTransaction.getTransactionHead ( ).getTimeStamp ( );//最初交易时间
                time = DateUtil.strToDate ( DateUtil.stampToDateHours ( time ) ).getTime ( );//转到时
                long hourCount = (DateUtil.getWebTime ( ) - time) / 3600000;
                if ( hourCount == 0 ) {
                    hourCount = 1L;
                }
                if ( hourCount >= 4320 ) {
                    time = DateUtil.getWebTime ( ) - (4320 * 3600000);
                    time = DateUtil.strToDate ( DateUtil.stampToDateHours ( time ) ).getTime ( );//转到时
                }
                Map<Long, Object> map = new TreeMap<> ( );
                List<Transaction> list = new ArrayList<> ( );
                for (int i = 1; i <= hourCount; i++) {
                    Long oneHourTime = time + (3600000);//一小时后的时间
                    for (Transaction transaction : transactionList) {
                        if ( transaction.getTransactionHead ( ).getTimeStamp ( ) >= time && transaction.getTransactionHead ( ).getTimeStamp ( ) < oneHourTime ) {
                            list.add ( transaction );
                        }
                    }
                    map.put ( time, list.size ( ) );
                    time = oneHourTime;
                    if ( i == hourCount ) {
                        if ( !list.isEmpty ( ) ) {
                            blockHeight = list.get ( list.size ( ) - 1 ).getBlockHeight ( );
                        } else {
                            blockHeight = lastTransaction.getBlockHeight ( );
                        }
                    }
                    list.clear ( );
                }
                String lastTime = "";
                if ( !map.isEmpty ( ) ) {
                    StringBuffer sb = new StringBuffer ( );
                    sb.append ( "\"datas\":[[" );
                    int i = 0;
                    int size = 1;//获取到map的大小
                    for (Long key : map.keySet ( )) {
                        if ( i == 0 ) {
                            Integer count = (Integer) map.get ( key );//获取到数量
                            sb.append ( key + "," );
                            sb.append ( count );
                            sb.append ( "]" );
                            i++;
                        } else {
                            sb.append ( "," );
                            sb.append ( "[" );
                            Integer count = (Integer) map.get ( key );//获取到数量
                            sb.append ( key + "," );
                            sb.append ( count );
                            sb.append ( "]" );
                        }
                        if ( size == map.size ( ) ) {
                            lastTime = key.toString ( );
                        }
                        size++;
                    }
                    sb.append ( "]" );
                    logger.info ( "【每小时交易数={}】", sb );
                    file.createNewFile ( );//创建文件
                    com.photon.photonchain.network.utils.FileUtil.writeFileContent ( path, sb + "||" + blockHeight + "--" + lastTime );
                }
            } else {
                String fileStr = com.photon.photonchain.network.utils.FileUtil.readToString ( path ).replace ( "\r\n", "" );//文本内容
                String mapStr = StringUtils.substringBetween ( fileStr, "[[", "]]" );
                Long fristTime = Long.valueOf ( StringUtils.substringBefore ( mapStr, "," ) );//第一次的时间
                String blockHeight = StringUtils.substringBetween ( fileStr, "||", "--" ).replace ( " ", "" );//获取到区块高度
                String lastTime = StringUtils.substringAfterLast ( fileStr, "--" ).replace ( " ", "" );//获取到上次最后的时间
                List<Transaction> newTranascTion = transactionRepository.findAllByGtBlockHeight ( Long.valueOf ( blockHeight ) );//查找出比文本中区块高度大的数据
                long time = Long.valueOf ( lastTime );
                long hourCount = (DateUtil.getWebTime ( ) - (time + 3599999)) / (3600000);//最后时间离现在相差几个小时
                long beforHour = (DateUtil.getWebTime ( ) - fristTime) / (3600000);
                boolean halfYear = false;//是否半年了
                if ( beforHour >= 4320 ) {
                    halfYear = true;
                }
                if ( hourCount >= 1 ) {
                    Map<Long, Object> map = new TreeMap<> ( );
                    List<Transaction> list = new ArrayList<> ( );
                    for (int i = 1; i <= hourCount; i++) {
                        Long oneHourTime = time + (3600001);//一小时后的时间
                        Long oneHourLater = oneHourTime + (3600000);//一小时后的时间
                        for (Transaction transaction : newTranascTion) {
                            if ( transaction.getTransactionHead ( ).getTimeStamp ( ) >= oneHourTime && transaction.getTransactionHead ( ).getTimeStamp ( ) < oneHourLater ) {
                                list.add ( transaction );
                            }
                        }
                        map.put ( oneHourTime, list.size ( ) );
                        time = oneHourTime;
                        if ( i == hourCount ) {
                            if ( !list.isEmpty ( ) ) {
                                blockHeight = list.get ( list.size ( ) - 1 ).getBlockHeight ( ) + "";
                            } else {
                                if ( !newTranascTion.isEmpty ( ) ) {
                                    blockHeight = newTranascTion.get ( newTranascTion.size ( ) - 1 ).getBlockHeight ( ) + "";
                                }
                            }
                        }
                        list.clear ( );
                    }
                    StringBuffer sb = new StringBuffer ( );
                    sb.append ( "\"datas\":[[" );
                    if ( halfYear ) {
                        for (int i = 0; i < (map.size ( ) * 2); i++) {
                            mapStr = StringUtils.substringAfter ( mapStr, "," );
                            if ( mapStr.startsWith ( "[" ) ) {
                                mapStr = StringUtils.substringAfter ( mapStr, "[" );
                            }
                        }
                    }
                    sb.append ( mapStr );
                    int i = 0;
                    int size = 1;
                    for (Long key : map.keySet ( )) {
                        if ( i == 0 ) {
                            sb.append ( "]" );
                            sb.append ( "," );
                            sb.append ( "[" );
                            Integer count = (Integer) map.get ( key );//获取到数量
                            sb.append ( key + "," );
                            sb.append ( count );
                            sb.append ( "]" );
                            i++;
                        } else {
                            sb.append ( "," );
                            sb.append ( "[" );
                            Integer count = (Integer) map.get ( key );//获取到数量
                            sb.append ( key + "," );
                            sb.append ( count );
                            sb.append ( "]" );
                        }
                        if ( size == map.size ( ) ) {
                            lastTime = key.toString ( );
                        }
                        size++;
                    }
                    sb.append ( "]" );
                    file.delete ( );//删除
                    file.createNewFile ( );//创建文件
                    logger.info ( "【每小时交易数={}】", sb );
                    if ( newTranascTion.isEmpty ( ) ) {
                        com.photon.photonchain.network.utils.FileUtil.writeFileContent ( path, sb.toString ( ) + "||" + blockHeight + "--" + lastTime );
                    } else {
                        com.photon.photonchain.network.utils.FileUtil.writeFileContent ( path, sb.toString ( ) + "||" + blockHeight + "--" + lastTime );
                    }
                }
            }
            long end = System.currentTimeMillis ( );
            System.out.println ( (end - start) / 1000 + "秒" );
        } catch (Exception e) {
            file.delete ( );
            e.printStackTrace ( );
        }
    }

    private void statisticalAssets() {
        Timer timer = new Timer ( );
        timer.schedule ( new TimerTask ( ) {
            @Override
            public void run() {
                Map<String, Map<String, Long>> accountAssets = statisticalAssetsManager.getStatisticalAssets ( );
                Long blockHeight = -1L;
                try {
                    blockHeight = accountAssets.get ( Constants.PTN ).get ( Constants.SYNC_BLOCK_HEIGHT );
                } catch (Exception e) {

                }
                Long nowBlockHeight = blockRepository.count ( );
                List<Transaction> transactionList = new ArrayList<> ( );
                if ( nowBlockHeight - blockHeight > 50000 ) {
                    int count = (int) (nowBlockHeight - blockHeight) / 50000;
                    for (int i = 0; i < count; i++) {
                        for (int j = 0; j < 5; j++) {
                            long start = blockHeight;
                            long end = blockHeight == -1 ? 10000 : blockHeight + 10000;
                            transactionRepository.findAllByBlockHeight ( start, end ).forEach ( transaction -> {
                                transactionList.add ( transaction );
                            } );
                            blockHeight = end;
                        }
                        logger.info ( "blockHeight" + blockHeight );
                        logger.info ( "transactionSize" + transactionList.size ( ) );
                    }
                    for (Transaction transaction : transactionList) {
                        String transFrom = transaction.getTransFrom ( ) + "_" + transaction.getTokenName ( );
                        String transTo = transaction.getTransTo ( ) + "_" + transaction.getTokenName ( );
                        Map<String, Long> fromAccount = accountAssets.get ( transFrom );
                        if ( fromAccount == null ) {
                            fromAccount = new HashMap<> ( );
                            fromAccount.put ( Constants.TOTAL_INCOME, 0L );
                            fromAccount.put ( Constants.TOTAL_EFFECTIVE_INCOME, 0L );
                            fromAccount.put ( Constants.TOTAL_EXPENDITURE, 0L );
                            fromAccount.put ( Constants.BALANCE, 0L );
                        }
                        Map<String, Long> toAccount = accountAssets.get ( transTo );
                        if ( toAccount == null ) {
                            toAccount = new HashMap<> ( );
                            toAccount.put ( Constants.TOTAL_INCOME, 0L );
                            toAccount.put ( Constants.TOTAL_EFFECTIVE_INCOME, 0L );
                            toAccount.put ( Constants.TOTAL_EXPENDITURE, 0L );
                            toAccount.put ( Constants.BALANCE, 0L );
                        }
                        switch (transaction.getTransType ( )) {
                            case 0:
                            case 5:
                            case 6:
                                fromAccount.put ( Constants.TOTAL_EXPENDITURE, fromAccount.get ( Constants.TOTAL_EXPENDITURE ) + transaction.getTransValue ( ) + transaction.getFee ( ) );
                                fromAccount.put ( Constants.BALANCE, fromAccount.get ( Constants.BALANCE ) - transaction.getTransValue ( ) - transaction.getFee ( ) );
                                toAccount.put ( Constants.TOTAL_INCOME, toAccount.get ( Constants.TOTAL_INCOME ) + transaction.getTransValue ( ) );
                                toAccount.put ( Constants.TOTAL_EFFECTIVE_INCOME, toAccount.get ( Constants.TOTAL_EFFECTIVE_INCOME ) + transaction.getTransValue ( ) );
                                toAccount.put ( Constants.BALANCE, toAccount.get ( Constants.BALANCE ) + transaction.getTransValue ( ) );
                                accountAssets.put ( transFrom, fromAccount );
                                accountAssets.put ( transTo, toAccount );
                                break;
                            case 1:
                                toAccount.put ( Constants.TOTAL_INCOME, transaction.getTransValue ( ) );
                                toAccount.put ( Constants.TOTAL_EFFECTIVE_INCOME, transaction.getTransValue ( ) );
                                toAccount.put ( Constants.BALANCE, transaction.getTransValue ( ) );
                                accountAssets.put ( transTo, toAccount );
                                break;
                            case 2:
                                toAccount.put ( Constants.TOTAL_INCOME, toAccount.get ( Constants.TOTAL_INCOME ) + transaction.getTransValue ( ) );
                                toAccount.put ( Constants.TOTAL_EFFECTIVE_INCOME, toAccount.get ( Constants.TOTAL_EFFECTIVE_INCOME ) + transaction.getTransValue ( ) );
                                toAccount.put ( Constants.BALANCE, toAccount.get ( Constants.BALANCE ) + transaction.getTransValue ( ) );
                                accountAssets.put ( transTo, toAccount );
                                break;
                            case 4:
                                fromAccount.put ( Constants.TOTAL_EXPENDITURE, fromAccount.get ( Constants.TOTAL_EXPENDITURE ) + transaction.getTransValue ( ) );
                                fromAccount.put ( Constants.BALANCE, fromAccount.get ( Constants.BALANCE ) - transaction.getTransValue ( ) );
                                toAccount.put ( Constants.TOTAL_INCOME, toAccount.get ( Constants.TOTAL_INCOME ) + transaction.getTransValue ( ) );
                                toAccount.put ( Constants.TOTAL_EFFECTIVE_INCOME, toAccount.get ( Constants.TOTAL_EFFECTIVE_INCOME ) + transaction.getTransValue ( ) );
                                toAccount.put ( Constants.BALANCE, toAccount.get ( Constants.BALANCE ) + transaction.getTransValue ( ) );
                                accountAssets.put ( transFrom, fromAccount );
                                accountAssets.put ( transTo, toAccount );
                                break;
                        }
                    }
                    Map<String, Long> hMap = new HashMap<> ( );
                    hMap.put ( Constants.SYNC_BLOCK_HEIGHT, blockHeight );
                    accountAssets.put ( Constants.PTN, hMap );
                    String statisticalAssetsPath = System.getProperty ( "user.home" ) + File.separator + "account" + File.separator + "statisticalAssets";
                    FileUtil.writeNewMap ( statisticalAssetsPath, accountAssets );
                    logger.info ( "统计完成" );
                }
            }
        }, 0, 1000 * 60 * 60 * 24 );
    }
}
