package com.photon.photonchain.network.core;

import com.photon.photonchain.network.ehcacheManager.InitializationManager;
import com.photon.photonchain.network.ehcacheManager.StatisticalAssetsManager;
import com.photon.photonchain.network.ehcacheManager.SyncBlockManager;
import com.photon.photonchain.network.utils.FileUtil;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.entity.Block;
import com.photon.photonchain.storage.entity.Token;
import com.photon.photonchain.storage.repository.BlockRepository;
import com.photon.photonchain.storage.repository.NodeAddressRepository;
import com.photon.photonchain.storage.repository.TokenRepository;
import com.photon.photonchain.storage.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Author:Lin
 * @Description:
 * @Date:20:06 2018/1/18
 * @Modified by:
 */
@Component
public class Initialization {
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private InitializationManager initializationManager;
    @Autowired
    private NodeAddressRepository nodeAddressRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private SyncBlockManager syncBlockManager;
    @Autowired
    private ResetData resetData;
    @Autowired
    private StatisticalAssetsManager statisticalAssetsManager;

    public void init() {
        Block lastBlock = null;
        while (lastBlock == null) {
            long blockHeight = blockRepository.count ( );
            lastBlock = blockRepository.findBlockByBlockId ( blockHeight - 1 );
            if ( lastBlock == null ) {
                resetData.backBlockByblockHeight ( blockHeight - 2 );
            }
        }
        initializationManager.setLastBlock ( lastBlock );
        String accountPath = System.getProperty ( "user.home" ) + File.separator + "account";
        Map<String, String> accountList = FileUtil.traverseFolder ( accountPath );
        initializationManager.setAccountList ( accountList );
        List<String> nodeList = nodeAddressRepository.findAllHexIp ( );
        initializationManager.setNodeList ( nodeList );
        String accountTokenInfoPath = System.getProperty ( "user.home" ) + File.separator + "account" + File.separator + "accountTokenInfo";
        File file = new File ( accountTokenInfoPath );
        String statisticalAssetsPath = System.getProperty ( "user.home" ) + File.separator + "account" + File.separator + "statisticalAssets";
        File assetsfile = new File ( statisticalAssetsPath );
        try {
            if ( !file.exists ( ) ) {
                file.createNewFile ( );
            }
            Map<String, Map<String, Long>> accountMap = new HashMap<> ( );
            if ( !assetsfile.exists ( ) ) {
                assetsfile.createNewFile ( );
            } else {
                List<String> assetsList = FileUtil.readFileByLines ( statisticalAssetsPath );
                for (String accountAssets : assetsList) {
                    String[] accountInfo = accountAssets.split ( "-" );
                    Map<String, Long> account = new HashMap<> ( );
                    if ( accountInfo.length == 5 ) {
                        account.put ( Constants.TOTAL_INCOME, Long.valueOf ( accountInfo[1] ) );
                        account.put ( Constants.TOTAL_EFFECTIVE_INCOME, Long.valueOf ( accountInfo[2] ) );
                        account.put ( Constants.TOTAL_EXPENDITURE, Long.valueOf ( accountInfo[3] ) );
                        account.put ( Constants.BALANCE, Long.valueOf ( accountInfo[4] ) );
                    } else if ( accountInfo.length == 2 ) {
                        account.put ( Constants.SYNC_BLOCK_HEIGHT, Long.valueOf ( accountInfo[1] ) );
                    }
                    accountMap.put ( accountInfo[0], account );
                }
            }
            statisticalAssetsManager.setStatisticalAssets ( accountMap );
        } catch (Exception e) {
        }
        //Token cache
        Iterator tokens = tokenRepository.findAll ( ).iterator ( );
        while (tokens.hasNext ( )) {
            Token token = (Token) tokens.next ( );
            initializationManager.addTokenDecimal ( token.getName ( ), token.getDecimals ( ) );
        }
        //set last transaction
//        Transaction lastTransaction = transactionRepository.findTransactionOne(new PageRequest(0, 1, Sort.Direction.DESC, "blockHeight")).get(0);
        initializationManager.setLastTransaction ( lastBlock.getBlockTransactions ( ).get ( lastBlock.getBlockTransactions ( ).size ( ) - 1 ) );
        //set FoundryMachine state
        initializationManager.setFoundryMachineState ( false );
        syncBlockManager.setSyncBlockSchedule ( Long.MAX_VALUE );
    }
}


