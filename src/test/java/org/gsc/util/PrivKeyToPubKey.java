package org.gsc.util;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.gsc.api.GrpcAPI;
import org.gsc.api.WalletGrpc;
import org.gsc.common.overlay.Parameter;
import org.gsc.common.overlay.discover.table.NodeEntry;
import org.gsc.common.utils.ByteArray;
import org.gsc.common.utils.Sha256Hash;
import org.gsc.common.utils.Utils;
import org.gsc.core.Constant;
import org.gsc.core.Wallet;
import org.gsc.core.exception.HeaderNotFound;
import org.gsc.core.wrapper.BlockWrapper;
import org.gsc.core.wrapper.TransactionWrapper;
import org.gsc.crypto.ECKey;
import org.gsc.db.Manager;
import org.gsc.protos.Contract;
import org.gsc.protos.Protocol;
import org.gsc.services.http.CreateAccountServlet;
import org.gsc.services.http.Util;
import org.junit.Test;
import org.spongycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

import static org.apache.commons.codec.digest.DigestUtils.sha256;

@Slf4j
public class PrivKeyToPubKey {



    @Test
    public void privKeyToPubKey() {
        String privStr = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4";
        BigInteger privKey = new BigInteger(privStr, 16);

        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
        final ECKey ecKey = ECKey.fromPrivate(privKey);
        byte[] address = ecKey.getAddress();

        String pubkey = Wallet.encode58Check(address);
        byte[] decodeAddr = Wallet.decodeFromBase58Check(pubkey);

        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Private Key: " + privStr);
        System.out.println("Address(Base58): " + Hex.toHexString(address));
        System.out.println("Public  Key: " + Hex.toHexString(ecKey.getPubKey()));
        System.out.println("Public  Key(Base58): " + pubkey);
        System.out.println();
        logger.info("---------------------------------------------");

        // String b="26cd2a3d9f938e13cd947ec05abc7fe734df8dd826";
        // String b="307830303030303030303030303030303030303030
        // String b="3078303030303030303030303030303030303030303030";
        String g="3078666666666666666666666666666666666666666666";
       // String Base58Address = "GNPhKboo7ez2MDH88qnwLGM5Vwr5vYSwb6";
        //byte[] Baddress = Wallet.decodeFromBase58Check(Base58Address);
        String Gaddress = Wallet.encode58Check(Hex.decode(g));

        System.out.println(Hex.toHexString(Wallet.decodeFromBase58Check("GcYjYPW92ezr3JUVh9exppDXaK4hzzuqnG")));
        logger.info("Baddress Key: " + Hex.toHexString(address));
        logger.info("Gaddress Key: " + Gaddress);
    }

    @Test
    public void aAddress(){
        String w="3078666666666666666666666666666666666666666666";
        logger.info(ByteString.copyFrom( "0xfffffffffffffffffffff".getBytes()).toStringUtf8());
        logger.info(Wallet.encode58Check("0x000000000000000000000".getBytes()));
        logger.info(Wallet.encode58Check(Hex.decode(w)));
        String t="0xfffffffffffffffffffff";

        String c="7YxAaK71utTpYJ8u4Zna7muWxd1pQwimpGxy8";
        String g="7YxAaK71utTpYJ8u4Zna7muWxd1pQwimpGxy8";
    }

    @Test
    public void generateAddress(){
        ECKey ecKey = new ECKey(Utils.getRandom());
        byte[] priKey = ecKey.getPrivKeyBytes();
        byte[] pubKey = ecKey.getPubKey();
        byte[] address = ecKey.getAddress();
        logger.info("Private Key: " + Hex.toHexString(priKey));
        logger.info("Public Key: " + Hex.toHexString(pubKey));
        logger.info("GSC Address: " + Wallet.encode58Check(address));
    }

    @Test
    public void createAccount(){
        String ownerPriKey = "ad146374a75310b9666e834ee4ad0866d6f4035967bfc76217c5a495fff9f0d0";
        byte[] ownerAddress = Hex.decode("266145c6b6ebb0a7a87a8ce1ef9ae8f21a7d5b24e7");

        byte[] newAccountAddress = Hex.decode("26582f10257c25bd4066f3cbec769643cbf12456d0");

        BigInteger privKey = new BigInteger(ownerPriKey, 16);

        Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
        final ECKey ecKey = ECKey.fromPrivate(privKey);

        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        String startNode = "127.0.0.1:50051";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Contract.AccountCreateContract.Builder accountCreateContract = Contract.AccountCreateContract.newBuilder();
        accountCreateContract.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        accountCreateContract.setAccountAddress(ByteString.copyFrom(newAccountAddress));

        Protocol.Transaction transaction = blockingStub.createAccount(accountCreateContract.build());

        Protocol.Transaction.Builder txSigned = transaction.toBuilder();
        byte[] rawData = transaction.getRawData().toByteArray();
        byte[] hash = sha256(rawData);
        List<Protocol.Transaction.Contract> contractList = transaction.getRawData().getContractList();
        for (int i = 0; i < contractList.size(); i++) {
            ECKey.ECDSASignature signature = ecKey.sign(hash);
            ByteString byteString = ByteString.copyFrom(signature.toByteArray());
            txSigned.addSignature(byteString);
        }

        Message message = blockingStub.broadcastTransaction(txSigned.build());
        logger.info(message.toString());
    }

    @Test
    public void trasfer(){
        String ownerPriKey = "ad146374a75310b9666e834ee4ad0866d6f4035967bfc76217c5a495fff9f0d0";
        byte[] ownerAddress = Hex.decode("266145c6b6ebb0a7a87a8ce1ef9ae8f21a7d5b24e7");

        byte[] toAddress = Hex.decode("26582f10257c25bd4066f3cbec769643cbf12456d0");

        BigInteger privKey = new BigInteger(ownerPriKey, 16);

        Wallet.setAddressPreFixByte(Byte.decode("0x26"));
        final ECKey ecKey = ECKey.fromPrivate(privKey);

        ManagedChannel channel = null;
        WalletGrpc.WalletBlockingStub blockingStub = null;

        String startNode = "127.0.0.1:50051";
        channel = ManagedChannelBuilder.forTarget(startNode).usePlaintext(true).build();
        blockingStub = WalletGrpc.newBlockingStub(channel);

        Contract.TransferContract.Builder transferContract = Contract.TransferContract.newBuilder();
        transferContract.setOwnerAddress(ByteString.copyFrom(ownerAddress));
        transferContract.setToAddress(ByteString.copyFrom(toAddress));
        transferContract.setAmount(1000000000000000L);

        Protocol.Transaction transaction = blockingStub.createTransaction(transferContract.build());

        TransactionWrapper transactionWrapper = new TransactionWrapper(transaction);
        System.out.println(transactionWrapper.getTransactionId());

        System.out.println( transaction.toString());
        Protocol.Transaction.Builder txSigned = transaction.toBuilder();
        byte[] rawData = transaction.getRawData().toByteArray();
        byte[] hash = sha256(rawData);
        List<Protocol.Transaction.Contract> contractList = transaction.getRawData().getContractList();
        for (int i = 0; i < contractList.size(); i++) {
            ECKey.ECDSASignature signature = ecKey.sign(hash);
            ByteString byteString = ByteString.copyFrom(signature.toByteArray());
            txSigned.addSignature(byteString);
        }

        Message message = blockingStub.broadcastTransaction(txSigned.build());

        logger.info(message.toString());
    }

    @Test
    public void addressTest() {
        String ownerAddress = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
        byte[] address = Hex.decode(ownerAddress);
        logger.info("---------------------------------------------");
        System.out.println();
        //System.out.println("Address: " + ByteString.copyFrom(address.toString());
        System.out.println();
        logger.info("---------------------------------------------");
    }

    @Test
    public void toHexString(){
        String str = "http://Mercury.org";
        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Hex String: " + Hex.toHexString(str.getBytes()));
        System.out.println();
        logger.info("---------------------------------------------");
    }

    @Test
    public void distanceOfTwoNode(){
        String node1 = "fa1b803793aba64c3ca784e21a604f9a4f94a0d002ba3bb7dfc8d2243bbaff32c34f6ea5b434cb5ceab66d6bce08f93733463ab0e722e6d693814bf070733196";
        String node2 = "3d07f0563743eecba23793b35d987b4f9079f7e4b658465b842a59ee0b9b35f11205f94ddbd7d66d26e9baea3b01ab139b1469ef0e30206222caaea22aeed3f7";
        String node3 = "ca39c31146a20f445a2bff58a8d42e5f24b2860105a8205e279cdcc256656021b4fdf0a366adc10e0c187560c5022456e8543982e912468cb6ad055edec29497";


        int distance12 = NodeEntry.distance(Hex.decode(node1), Hex.decode(node2));
        int distance23 = NodeEntry.distance(Hex.decode(node2), Hex.decode(node3));
        int distance13 = NodeEntry.distance(Hex.decode(node1), Hex.decode(node3));
        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Node distance distance12: " + distance12);
        System.out.println("Node distance distance23: " + distance23);
        System.out.println("Node distance distance13: " + distance13);
        System.out.println();
        logger.info("---------------------------------------------");
    }

    @Test
    public void ByteToString(){
        String str = "56616c6964617465205472616e73666572436f6e7472616374206572726f722c2062616c616e6365206973206e6f742073756666696369656e742e";
        logger.info("---------------------------------------------");
        System.out.println();
        System.out.println("Hex String: " + hexStr2Str(str));
        System.out.println();
        logger.info("---------------------------------------------");
    }

    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;
        for (int i = 0; i < bytes.length;i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }

    @Test
    public void sign(){

        Manager dbManager = null; // @Autowired

        String privStr = "ad146374a75310b9666e834ee4ad0866d6f4035967bfc76217c5a495fff9f0d0";
        BigInteger privKey = new BigInteger(privStr, 16);

        ECKey ecKey = ECKey.fromPrivate(privKey);

        Contract.TransferContract.Builder transferContract = Contract.TransferContract.newBuilder();
        transferContract.setOwnerAddress(ByteString.copyFrom(ecKey.getAddress()));
        transferContract.setToAddress(ByteString.copyFrom(new ECKey(Utils.getRandom()).getAddress()));
        transferContract.setAmount(1000000000000000L);

        TransactionWrapper transactionWrapper =  new TransactionWrapper(transferContract.build(),
                Protocol.Transaction.Contract.ContractType.TransferContract);

        try {
            BlockWrapper headBlock = null;
            List<BlockWrapper> blockList = dbManager.getBlockStore().getBlockByLatestNum(1);
            if (CollectionUtils.isEmpty(blockList)) {
                logger.error("latest block not found");
            } else {
                headBlock = blockList.get(0);
            }
            transactionWrapper.setReference(headBlock.getNum(), headBlock.getBlockId().getBytes());
            long expiration = headBlock.getTimeStamp() + Constant.TRANSACTION_DEFAULT_EXPIRATION_TIME;
            transactionWrapper.setExpiration(expiration);
            transactionWrapper.setTimestamp();
        } catch (Exception e) {
            logger.error("Header not found.");
            e.printStackTrace();
        }

        Protocol.Transaction transaction = transactionWrapper.getInstance();

        Protocol.Transaction.Builder txSigned = transaction.toBuilder();
        byte[] rawData = transaction.getRawData().toByteArray();
        byte[] hash = sha256(rawData);
        List<Protocol.Transaction.Contract> contractList = transaction.getRawData().getContractList();
        for (int i = 0; i < contractList.size(); i++) {
            ECKey.ECDSASignature signature = ecKey.sign(hash);
            ByteString byteString = ByteString.copyFrom(signature.toByteArray());
            txSigned.addSignature(byteString);
        }

        logger.info("\n Transcation: " + Util.printTransaction(txSigned.build()));
    }

}
