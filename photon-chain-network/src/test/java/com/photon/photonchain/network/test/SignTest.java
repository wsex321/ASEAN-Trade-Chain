package com.photon.photonchain.network.test;

import com.alibaba.fastjson.JSON;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.encryption.ECKey.ECDSASignature;
import com.photon.photonchain.storage.encryption.HashMerkle;
import com.photon.photonchain.storage.encryption.SHAEncrypt;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.photon.photonchain.storage.entity.Block;
import com.photon.photonchain.storage.entity.BlockHead;
import com.photon.photonchain.storage.entity.Transaction;
import com.photon.photonchain.storage.entity.TransactionHead;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import javax.sound.midi.Soundbank;

/**
 * @author Wu Created by SKINK on 2018/2/2.
 */

public class SignTest {

  private static final long RECEIVE_QUANTITY = 2000000000L * Constants.MININUMUNIT;
  private static final String HASH_PREV_BLOCK = "aced000570";

  public static void main(String[] args) {
    byte[] genesisAccount = Hex.decode("04cca1f501b7237ba143b429262ba4dc78a3ae47ffb9b2e4a29230516a8cb43952ff01cfa901f0207079b71f43415f6274b38de4bac686e7af432451b661f411d4");
    byte[] genesisPriKey = Hex.decode("61e103e2820895145d9060932794aeafb769369efc34da8758b3b0623f4e95c2");
    byte[] acceptAccount = Hex.decode("04cf6bcbac4b12dcd00951fb2998e7e9e8410a18807c7525cb9c8adae1de3d81667c8c4c2fe61b5ea814fa09713c1a0ca0338e3676625cb8937c24cec695be53b4");

    TransactionHead transactionHead = new TransactionHead(Hex.toHexString(genesisAccount), Hex.toHexString(acceptAccount), RECEIVE_QUANTITY, 0, Constants.GENESIS_TIME);
    Transaction transaction = new Transaction(null, transactionHead, 0, 0, Hex.toHexString(genesisAccount), Hex.toHexString(acceptAccount), "", Constants.PTN, 1, RECEIVE_QUANTITY, 0);
    byte[] transSignature = JSON.toJSONString(ECKey.fromPrivate(genesisPriKey).sign(SHAEncrypt.sha3(SerializationUtils.serialize(transaction.toSignature())))).getBytes();
    transaction.setTransSignature(transSignature);
    System.out.println(Hex.toHexString(transSignature));
    List<Transaction> transactionList = new ArrayList<>();
    transactionList.add(transaction);

    List<byte[]> transactionSHAList = new ArrayList<>();
    long blockSize = 0;
    for (Transaction transactionF : transactionList) {
      TransactionHead transactionHeadF = transactionF.getTransactionHead();
      transactionSHAList.add(SHAEncrypt.SHA256(transactionHeadF.toString()));
      blockSize = blockSize + SerializationUtils.serialize(transactionHeadF).length;
    }

    byte[] hashMerkleRoot = transactionSHAList.isEmpty() ? new byte[]{} : HashMerkle.getHashMerkleRoot(transactionSHAList);
    System.out.println(Hex.toHexString(hashMerkleRoot));
    BlockHead blockHead = new BlockHead(Constants.BLOCK_VERSION, Constants.GENESIS_TIME, Constants.CUMULATIVE_DIFFICULTY, Hex.decode(HASH_PREV_BLOCK), hashMerkleRoot);
    byte[] blockSignature = JSON.toJSONString(ECKey.fromPrivate(genesisPriKey).sign(SHAEncrypt.sha3(SerializationUtils.serialize(blockHead)))).getBytes();
    System.out.println(Hex.toHexString(blockSignature));
    Block block = new Block(0, 441, RECEIVE_QUANTITY, 0, Hex.decode(Hex.toHexString(blockSignature)), genesisAccount, blockHead, transactionList);
    block.setBlockHash(Hex.toHexString(SHAEncrypt.SHA256(block.getBlockHead())));
    System.out.println(block.getBlockHash());

  }
}
