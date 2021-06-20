// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private final HashMap<ByteArrayWrapper, BlockNode> blockChain;
    private BlockNode maxHeightNode;
    private final TransactionPool txPool;
    private int oldestBlockHeight;

    /**
     * Private class of block node
     */
    private static class BlockNode {
        public Block block;
        public int height;
        public UTXOPool utxoPool;
        public BlockNode parent;
        public ArrayList<BlockNode> children;

        // constructor
        public BlockNode(Block block, BlockNode parent, UTXOPool utxoPool) {
            this.block = block;
            this.parent = parent;
            this.utxoPool = utxoPool;
            this.children = new ArrayList<>();
            if (this.parent == null) {
                this.height = 1;
            }
            else {
                this.height = this.parent.height + 1;
                this.parent.children.add(this);
            }
        }
    }

    /**
     * Create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        blockChain = new HashMap<>();
        UTXOPool utxoPool = new UTXOPool();
        txPool = new TransactionPool();

        // add coinbase utxos into utxoPool
        Transaction coinbase = genesisBlock.getCoinbase();
        for (int i = 0; i < coinbase.numOutputs(); i++) {
            Transaction.Output out = coinbase.getOutput(i);
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            utxoPool.addUTXO(utxo, out);
        }

        // create a blockNode for genesisBlock
        BlockNode genesisNode = new BlockNode(genesisBlock, null, utxoPool);

        // wrap the hash of genesisBlock, and add the block into blockChain
        ByteArrayWrapper wrappedHash = new ByteArrayWrapper(genesisBlock.getHash());
        blockChain.put(wrappedHash, genesisNode);

        // the maximum height block node
        maxHeightNode = genesisNode;
        oldestBlockHeight = 1;
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return maxHeightNode.block;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return maxHeightNode.utxoPool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return txPool;
    }

    /**
     * Get the height of oldest node
     */
    public int getOldestBlockHeight() {
        return oldestBlockHeight;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS

        // get parent node
        byte[] prevBlockHash = block.getPrevBlockHash();
        if (prevBlockHash == null) {
            // it is the genesis block
            return false;
        }
        ByteArrayWrapper wrappedPrevBlockHash = new ByteArrayWrapper(prevBlockHash);
        BlockNode parent = this.blockChain.get(wrappedPrevBlockHash);
        if (parent == null) {
            // the parent node is null
            return false;
        }

        TxHandler txHandler = new TxHandler(parent.utxoPool);
        int numberOfTx = block.getTransactions().size();
        Transaction[] txList = new Transaction[numberOfTx];

        // check for validity of the block
        for (int i = 0; i < numberOfTx; i++) {
            txList[i] = block.getTransaction(i);
        }
        Transaction[] validTx = txHandler.handleTxs(txList);
        if (validTx.length != numberOfTx) {
            System.out.println("Invalid transaction");
            return false;
        }

        // check for block height
        if (parent.height + 1 <= maxHeightNode.height - CUT_OFF_AGE) {
            System.out.println("Invalid height");
            return false;
        }

        // add coinbase utxos into utxoPool
        Transaction coinbase = block.getCoinbase();
        for (int i = 0; i < coinbase.numOutputs(); i++) {
            Transaction.Output out = coinbase.getOutput(i);
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            txHandler.getUtxoPool().addUTXO(utxo, out);
        }

        // update txPool
        List<Transaction> transactions = block.getTransactions();
        for (Transaction transaction : transactions) {
            txPool.removeTransaction(transaction.getHash());
        }

        // wrap the hash of the block, and add it into blockChain
        BlockNode newBlockNode = new BlockNode(block, parent, txHandler.getUtxoPool());
        ByteArrayWrapper wrappedHash = new ByteArrayWrapper(block.getHash());
        blockChain.put(wrappedHash, newBlockNode);

        // update maxHeightNode
        if (parent.height + 1 > maxHeightNode.height) {
            maxHeightNode = newBlockNode;
        }

        // Keep around the most recent blocks
        // here the exact number of blocks to store is 10
        if (maxHeightNode.height - oldestBlockHeight >= 9) {
            Iterator<ByteArrayWrapper> NodeIter = blockChain.keySet().iterator();
            while (NodeIter.hasNext()) {
                ByteArrayWrapper key = NodeIter.next();
                BlockNode blockNode = blockChain.get(key);
                if (blockNode.height <= maxHeightNode.height - 9) {
                    NodeIter.remove();
                }
            }
            oldestBlockHeight = maxHeightNode.height - 8;
        }

        return true;
    }


    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        txPool.addTransaction(tx);
    }
}