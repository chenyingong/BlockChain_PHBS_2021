import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.security.*;


public class TxHandlerTest {
    static KeyPair kpAlice;
    static KeyPair kpBob;
    static KeyPair kpCal;

    @BeforeClass
    public static void beforeClass() {

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpAlice = kpg.generateKeyPair();
            kpBob = kpg.generateKeyPair();
            kpCal = kpg.generateKeyPair();
        }
        catch(GeneralSecurityException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Test if all outputs claimed by transactions are in the current UTXO pool.
     */
    @Test
    public void testOutputsInUTXOPool() {

        // Creates 100 coins to Alice
        Transaction createCoinTx = new Transaction();
        createCoinTx.addOutput(100, kpAlice.getPublic());
        createCoinTx.finalize();
        // Output of createCoinTx is NOT put in UTXO pool
        UTXOPool utxoPool = new UTXOPool();
        TxHandler txHandler = new TxHandler(utxoPool);

        // Alice transfers 10 coins to Bob, 20 to Cal
        Transaction tx1 = new Transaction();
        tx1.addInput(createCoinTx.getHash(), 0);
        tx1.addOutput(10, kpBob.getPublic());
        tx1.addOutput(20, kpCal.getPublic());

        // Sign for tx1
        byte[] sig1 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpAlice.getPrivate());
            sig.update(tx1.getRawDataToSign(0));
            sig1 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx1.getInput(0).addSignature(sig1);
        tx1.finalize();

        // UTXO pool does not contains the UTXO
        assertFalse(txHandler.isValidTx(tx1));

        // Put output of createCoinTx to the pool
        UTXO utxo = new UTXO(createCoinTx.getHash(), 0);
        utxoPool.addUTXO(utxo, createCoinTx.getOutput(0));
        TxHandler txHandler1 = new TxHandler(utxoPool);

        // UTXO pool now contains the UTXO
        assertTrue(txHandler1.isValidTx(tx1));
    }

    /**
     * Test the signatures on each input of transactions are valid.
     */
    @Test
    public void testValidSig() {

        // Creates 100 coins to Alice
        Transaction createCoinTx = new Transaction();
        createCoinTx.addOutput(100, kpAlice.getPublic());
        createCoinTx.finalize();
        // Output of createCoinTx is put in UTXO pool
        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(createCoinTx.getHash(), 0);
        utxoPool.addUTXO(utxo, createCoinTx.getOutput(0));
        TxHandler txHandler = new TxHandler(utxoPool);

        // Alice transfers 10 coins to Bob, 20 to Cal
        Transaction tx1 = new Transaction();
        tx1.addInput(createCoinTx.getHash(), 0);
        tx1.addOutput(10, kpBob.getPublic());
        tx1.addOutput(20, kpCal.getPublic());

        // Sign for tx1 with Bob's kp, which is incorrect
        byte[] sig1 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpBob.getPrivate());
            sig.update(tx1.getRawDataToSign(0));
            sig1 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx1.getInput(0).addSignature(sig1);
        tx1.finalize();

        assertFalse(txHandler.isValidTx(tx1));

        // Sign for tx1 with Alice's kp, which is now correct
        byte[] sig2 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpAlice.getPrivate());
            sig.update(tx1.getRawDataToSign(0));
            sig2 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx1.getInput(0).addSignature(sig2);
        tx1.finalize();

        assertTrue(txHandler.isValidTx(tx1));
    }

    /**
     * Test for double spending.
     */
    @Test
    public void testNoDoubleSpending() {

        // Creates 100 coins to Alice
        Transaction createCoinTx = new Transaction();
        createCoinTx.addOutput(100, kpAlice.getPublic());
        createCoinTx.finalize();
        // Output of createCoinTx is put in UTXO pool
        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(createCoinTx.getHash(), 0);
        utxoPool.addUTXO(utxo, createCoinTx.getOutput(0));
        TxHandler txHandler = new TxHandler(utxoPool);

        // Alice transfers 10 coins to Bob, 20 to Cal
        Transaction tx1 = new Transaction();
        tx1.addInput(createCoinTx.getHash(), 0);
        tx1.addOutput(10, kpBob.getPublic());
        tx1.addOutput(20, kpCal.getPublic());

        // Alice wants double spending: transfers 10 coins to Bob, 20 to Cal again
        tx1.addInput(createCoinTx.getHash(), 0);
        tx1.addOutput(10, kpBob.getPublic());
        tx1.addOutput(20, kpCal.getPublic());

        // Sign for tx1: input 0
        byte[] sig1 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpAlice.getPrivate());
            sig.update(tx1.getRawDataToSign(0));
            sig1 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx1.getInput(0).addSignature(sig1);

        // Sign for tx1: input 1
        byte[] sig2 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpAlice.getPrivate());
            sig.update(tx1.getRawDataToSign(0));
            sig2 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx1.getInput(1).addSignature(sig2);
        tx1.finalize();

        assertFalse(txHandler.isValidTx(tx1));
    }

    /**
     * Test for non-negative output values.
     */
    @Test
    public void testValidOutputValues() {

        // Creates 100 coins to Alice
        Transaction createCoinTx = new Transaction();
        createCoinTx.addOutput(100, kpAlice.getPublic());
        createCoinTx.finalize();
        // Output of createCoinTx is put in UTXO pool
        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(createCoinTx.getHash(), 0);
        utxoPool.addUTXO(utxo, createCoinTx.getOutput(0));
        TxHandler txHandler = new TxHandler(utxoPool);

        // Alice transfers 10 coins to Bob, -20 to Cal
        Transaction tx1 = new Transaction();
        tx1.addInput(createCoinTx.getHash(), 0);
        tx1.addOutput(10, kpBob.getPublic());
        tx1.addOutput(-20, kpCal.getPublic());

        // Sign for tx1 with Alice's kp
        byte[] sig1 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpAlice.getPrivate());
            sig.update(tx1.getRawDataToSign(0));
            sig1 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx1.getInput(0).addSignature(sig1);
        tx1.finalize();

        assertFalse(txHandler.isValidTx(tx1));
    }

    /**
     * Test for if the sum of input values is greater than or equal to the sum of its output.
     */
    @Test
    public void testValidBalance() {

        // Creates 100 coins to Alice
        Transaction createCoinTx = new Transaction();
        createCoinTx.addOutput(100, kpAlice.getPublic());
        createCoinTx.finalize();
        // Output of createCoinTx is put in UTXO pool
        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(createCoinTx.getHash(), 0);
        utxoPool.addUTXO(utxo, createCoinTx.getOutput(0));
        TxHandler txHandler = new TxHandler(utxoPool);

        // Alice transfers 10 coins to Bob, 100 to Cal
        Transaction tx1 = new Transaction();
        tx1.addInput(createCoinTx.getHash(), 0);
        tx1.addOutput(10, kpBob.getPublic());
        tx1.addOutput(100, kpCal.getPublic());

        // Sign for tx1 with Alice's kp
        byte[] sig1 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpAlice.getPrivate());
            sig.update(tx1.getRawDataToSign(0));
            sig1 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx1.getInput(0).addSignature(sig1);
        tx1.finalize();

        assertFalse(txHandler.isValidTx(tx1));
    }

    /**
     * Test for method: handleTxs
     */
    @Test
    public void testHandleTxs() {

        // Creates 100 coins to Alice
        Transaction createCoinTx = new Transaction();
        createCoinTx.addOutput(100, kpAlice.getPublic());
        createCoinTx.finalize();
        // Output of createCoinTx is put in UTXO pool
        UTXOPool utxoPool = new UTXOPool();
        UTXO utxo = new UTXO(createCoinTx.getHash(), 0);
        utxoPool.addUTXO(utxo, createCoinTx.getOutput(0));
        TxHandler txHandler = new TxHandler(utxoPool);

        // Alice transfers 10 coins to Bob, 20 to Cal
        Transaction tx1 = new Transaction();
        tx1.addInput(createCoinTx.getHash(), 0);
        tx1.addOutput(10, kpBob.getPublic());
        tx1.addOutput(20, kpCal.getPublic());

        // Sign for tx1
        byte[] sig1 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpAlice.getPrivate());
            sig.update(tx1.getRawDataToSign(0));
            sig1 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx1.getInput(0).addSignature(sig1);
        tx1.finalize();

        // Cal transfers 5 coins to Bob, Bob transfers 2 coins to Alice
        Transaction tx2 = new Transaction();
        tx2.addInput(tx1.getHash(), 0);  // index 0 refers to output 1 of tx1
        tx2.addInput(tx1.getHash(), 1);  // index 1 refers to output 1 of tx1
        tx2.addOutput(2, kpAlice.getPublic());
        tx2.addOutput(5, kpBob.getPublic());

        // Sign for tx2
        byte[] sig2 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpBob.getPrivate());
            sig.update(tx2.getRawDataToSign(0));
            sig2 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx2.getInput(0).addSignature(sig2);

        byte[] sig3 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpCal.getPrivate());
            sig.update(tx2.getRawDataToSign(1));
            sig3 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx2.getInput(1).addSignature(sig3);
        tx2.finalize();

        // Alice transfers 3 coins to Cal
        Transaction tx3 = new Transaction();
        tx3.addInput(tx2.getHash(), 0);
        tx3.addOutput(3, kpCal.getPublic());

        // Sign for tx3
        byte[] sig4 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpAlice.getPrivate());
            sig.update(tx3.getRawDataToSign(0));
            sig4 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx3.getInput(0).addSignature(sig4);
        tx3.finalize();

        Transaction[] acceptedTx = txHandler.handleTxs(new Transaction[] {tx1, tx2, tx3});
        // tx1, tx2 supposed to be valid, tx3 supposed to be invalid
        assertEquals(acceptedTx.length, 2);
        // assertFalse(txHandler.isValidTx(tx3));
    }
}