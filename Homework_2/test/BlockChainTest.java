
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;


public class BlockChainTest {
    static KeyPair kpAlice;
    static KeyPair kpBob;
    static KeyPair kpCal;

    @BeforeClass
    public static void setUpBeforeClass() {

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
     * Test whether blockchain can identify invalid transactions.
     */
    @Test
    public void testValidTxs() {
        // initialize the genesis block, the coinbase transaction would go to Alice
        Block genesisBlock = new Block(null, kpAlice.getPublic());
        genesisBlock.finalize();

        // add genesis block into block chain, and initialize the block handler
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        // transaction tx, which is invalid (signed by Bob, but should be by Alice)
        Transaction tx = new Transaction();
        tx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        tx.addOutput(10, kpBob.getPublic());

        // Sign for tx
        byte[] sig1 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpBob.getPrivate());
            sig.update(tx.getRawDataToSign(0));
            sig1 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx.getInput(0).addSignature(sig1);
        tx.finalize();

        // create a new block added by Cal
        Block block = new Block(genesisBlock.getHash(), kpCal.getPublic());
        block.addTransaction(tx);
        block.finalize();

        assertFalse(blockHandler.processBlock(block));

        // transaction tx1, which is valid
        Transaction tx1 = new Transaction();
        tx1.addInput(genesisBlock.getCoinbase().getHash(), 0);
        tx1.addOutput(10, kpBob.getPublic());

        // Sign for tx1
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

        // create a new block added by Cal
        Block block1 = new Block(genesisBlock.getHash(), kpCal.getPublic());
        block1.addTransaction(tx1);
        block1.finalize();

        assertTrue(blockHandler.processBlock(block1));
    }

    /**
     * Test whether the previous block hash of the block is correct.
     */
    @Test
    public void testCorrectPrevBlockHash() {
        // initialize the genesis block, the coinbase transaction would go to Alice
        Block genesisBlock = new Block(null, kpAlice.getPublic());
        genesisBlock.finalize();

        // add genesis block into block chain, and initialize the block handler
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        // create a new block with correct hash value
        byte[] genesisBlockHash = genesisBlock.getHash();
        Block block1 = new Block(genesisBlockHash , kpCal.getPublic());
        block1.finalize();

        assertTrue(blockHandler.processBlock(block1));

        // wrong prevBlockHash
        byte[] wrong_hash = Arrays.copyOf(genesisBlockHash, genesisBlockHash.length);
        wrong_hash[0] *= 0.5;

        // create a new block with incorrect hash value
        Block block2 = new Block(wrong_hash, kpCal.getPublic());
        block2.finalize();

        assertFalse(blockHandler.processBlock(block2));
    }

    /**
     * Test whether previous block hash value is in coincidence with PrevBlockHash.
     */
    @Test
    public void testEqualHash() {
        // initialize the genesis block, the coinbase transaction would go to Alice
        Block genesisBlock = new Block(null, kpAlice.getPublic());
        genesisBlock.finalize();

        // add genesis block into block chain, and initialize the block handler
        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        // transaction tx, which is invalid (signed by Bob, but should be by Alice)
        Transaction tx = new Transaction();
        tx.addInput(genesisBlock.getCoinbase().getHash(), 0);
        tx.addOutput(10, kpBob.getPublic());

        // Sign for tx
        byte[] sig1 = null;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(kpAlice.getPrivate());
            sig.update(tx.getRawDataToSign(0));
            sig1 = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        tx.getInput(0).addSignature(sig1);
        tx.finalize();

        // create a new block added by Cal
        Block block = new Block(genesisBlock.getHash(), kpCal.getPublic());
        block.addTransaction(tx);
        block.finalize();

        blockHandler.processBlock(block);

        // create a new block by Alice
        Block block1 = blockHandler.createBlock(kpAlice.getPublic());

        assertEquals(block1.getPrevBlockHash(), block.getHash());
    }

    /**
     * Test whether the block storage is well-behaved.
     */
    @Test
    public void testStorage() {

        int num_of_blocks_in_chain = 11;

        Block genesisBlock = new Block(null, kpAlice.getPublic());
        genesisBlock.finalize();

        BlockChain blockChain = new BlockChain(genesisBlock);
        BlockHandler blockHandler = new BlockHandler(blockChain);

        for (int i = 0; i < num_of_blocks_in_chain; i++) {
            blockHandler.createBlock(kpBob.getPublic());
        }

        assertTrue(blockChain.getOldestBlockHeight() == num_of_blocks_in_chain+1-8);
    }

}