package com.photon.photonchain.network.test;

import com.alibaba.fastjson.JSONObject;
import com.photon.photonchain.network.utils.DeEnCode;
import com.photon.photonchain.network.utils.FileUtil;
import com.photon.photonchain.network.utils.FoundryUtils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import com.photon.photonchain.network.utils.NetWorkUtil;
import com.photon.photonchain.storage.constants.Constants;
import com.photon.photonchain.storage.encryption.ECKey;
import com.photon.photonchain.storage.encryption.SHAEncrypt;
import org.spongycastle.util.encoders.Hex;

/**
 * @author Wu Created by SKINK on 2018/2/28.
 */
public class TimeTest {

  public static void main(String[] args) throws IOException {

    String jsonStr = "B\u0012\u0015\u0006BZWPW[SPTWB\u0012\u0017 )\u0007\u001BBRVP\u0007[S\u0004  VR\u0003\u0001\u0004P[\u0006RW[U\u0007\u0007R\u0001W\u0006T\u0001Q\u0001\u0007\u0004\u0007\u0004ZV\u0007\u0006\u0001\u0001\u0007 \u0007ZP \u0007[T\u0001Z\u0006TZ[ \u0007PZ\u0003\u0004\u0004TU\u0007WQ\u0001\u0006\u0001[UZUV  \u0003Z\u0001VSTVV\u0006[QR[VRVZ[\u0007R\u0007 [Z[R\u0007Q[RT\u0004TU\u0001\u0003\u0006S \u0007R\u0001V\u0006U\u0004R\u0007Q\u0003R B\u0012\u0010\u000B)\u0007\u001BBZPTRU\u0001Q[Q\u0007\u0001 RRQ TR\u0003WUZ\u0003 WVR\u0004W[S[\u0007TRUP\u0001SP\u0006PV[WTPV\u0001WP QTQ\u0007RS\u0001S\u0006\u0003\u0007\u0004\n";


    String account = DeEnCode.decode(jsonStr);
    String pubkey = account.substring(account.indexOf(Constants.PUBKEY_FLAG) + Constants.PUBKEY_FLAG.length(), account.indexOf(Constants.PRIKEY_FLAG));
    String address = ECKey.pubkeyToAddress(pubkey);
    System.out.println(account);


    String pubkeyW = "049db06bcdcc28045bb25c04445e6aad91fb929197ec384d21d8fa04e29b1bd759f96251f31b4858d9fdd9d3db630285dc0bb593c24936663015ed0622ea8721c0";
    System.out.println(ECKey.pubkeyToAddress(pubkeyW));

  }



}
