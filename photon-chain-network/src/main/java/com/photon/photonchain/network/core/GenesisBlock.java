package com.photon.photonchain.network.core;


import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.encryption.SHAEncrypt;
import com.photon.photonchain.storage.entity.*;
import com.photon.photonchain.storage.repository.BlockRepository;
import com.photon.photonchain.storage.repository.NodeAddressRepository;
import com.photon.photonchain.storage.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author:Lin
 * @Description:
 * @Date:16:09 2018/1/12
 * @Modified by:
 */
@Component
public class GenesisBlock {
    final static Logger logger = LoggerFactory.getLogger(GenesisBlock.class);


    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private NodeAddressRepository nodeAddressRepository;

    private static final byte[] GENESIS_PUBLIC_KEY = {4, -52, -95, -11, 1, -73, 35, 123, -95, 67, -76, 41, 38, 43, -92, -36, 120, -93, -82, 71, -1, -71, -78, -28, -94, -110, 48, 81, 106, -116, -76, 57, 82, -1, 1, -49, -87, 1, -16, 32, 112, 121, -73, 31, 67, 65, 95, 98, 116, -77, -115, -28, -70, -58, -122, -25, -81, 67, 36, 81, -74, 97, -12, 17, -44};
    public static final byte[] ACCEPTER = {4, -49, 107, -53, -84, 75, 18, -36, -48, 9, 81, -5, 41, -104, -25, -23, -24, 65, 10, 24, -128, 124, 117, 37, -53, -100, -118, -38, -31, -34, 61, -127, 102, 124, -116, 76, 47, -26, 27, 94, -88, 20, -6, 9, 113, 60, 26, 12, -96, 51, -114, 54, 118, 98, 92, -72, -109, 124, 36, -50, -58, -107, -66, 83, -76};
    private static final String HASH_PREV_BLOCK = "aced000570";
    public static final long RECEIVE_QUANTITY = 2000000000L * Constants.MININUMUNIT;
    public static final long GENESIS_TIME = Constants.GENESIS_TIME;
    private static final String TRANS_SIGNATURE = "7b2272223a32343539333230353535343838323538353638323837383333303938383933333130333236373538373439323534373634353434383134303236373032303732363133323537333138383037302c2273223a31363430393338313230353235373830323732363538373533353631323130373033323238343836333035343333393238323132343535303735313336373531323936353332393633323530312c2276223a32377d";
    private static final String HASH_MERKLE_ROOT = "61353133353439663236656266373137633866633362666564626434623836336462373835353230393366363937303537316335323433663561633735383063";
    private static final String BLOCK_SIGNATURE = "7b2272223a32343633363831323239353533313134373232383236383931393232343230393232393339373531343532363233303137333931363233383230333030333736383431313834343935343436322c2273223a363730373637343535333536383837303439333330393332383634313537353336363630393831333339383530393632333439333938353939393639393531313136363636343833343730362c2276223a32387d";

    @Transactional(rollbackFor = Exception.class)
    public void init() {
        if (blockRepository.count() == 0) {
            logger.info("MainAccount:" + ECKey.pubkeyToAddress(Hex.toHexString(ACCEPTER)));
            logger.info("GenesisAccount:" + ECKey.pubkeyToAddress(Hex.toHexString(GENESIS_PUBLIC_KEY)));
            TransactionHead transactionHead = new TransactionHead(Hex.toHexString(GENESIS_PUBLIC_KEY), Hex.toHexString(ACCEPTER), RECEIVE_QUANTITY, 0, GENESIS_TIME);
            BlockHead blockHead = new BlockHead(Constants.BLOCK_VERSION, GENESIS_TIME, Constants.CUMULATIVE_DIFFICULTY, Hex.decode(HASH_PREV_BLOCK), Hex.decode(HASH_MERKLE_ROOT));
            Transaction transaction = new Transaction(Hex.decode(TRANS_SIGNATURE), transactionHead, 0, 0, Hex.toHexString(GENESIS_PUBLIC_KEY), Hex.toHexString(ACCEPTER), "", Constants.PTN, 1, RECEIVE_QUANTITY, 0);
            List<Transaction> transactionList = new ArrayList<>();
            transactionList.add(transaction);
            Block block = new Block(0, 441, RECEIVE_QUANTITY, 0, Hex.decode(BLOCK_SIGNATURE), GENESIS_PUBLIC_KEY, blockHead, transactionList);
            block.setBlockHash(Hex.toHexString(SHAEncrypt.SHA256(blockHead)));
            transactionRepository.save(transaction);
            blockRepository.save(block);
        }
    }
}
