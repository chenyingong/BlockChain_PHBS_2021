# Block Chain and Digital Currency, Homework #2
Chenyin Gong
1901212480
2021-06-20

## Summary
### 1. `BlockNode` a data structure containing block, its parent block node, and a UTXO pool 
```java
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
```

### 2. `BlockChain` Create an empty block chain with just a genesis block. Assume `genesisBlock` is a valid block
- Add coinbase's UTXOs into UTXO pool 
```java
Transaction coinbase = genesisBlock.getCoinbase();
for (int i = 0; i < coinbase.numOutputs(); i++) {
    Transaction.Output out = coinbase.getOutput(i);
    UTXO utxo = new UTXO(coinbase.getHash(), i);
    utxoPool.addUTXO(utxo, out);
}
```
- Create a block node for the genesisBlock
```java
BlockNode genesisNode = new BlockNode(genesisBlock, null, utxoPool);
```
- Wrap the hash of genesisBlock, and add the node into blockChain
```java
ByteArrayWrapper wrappedHash = new ByteArrayWrapper(genesisBlock.getHash());
blockChain.put(wrappedHash, genesisNode);
```
- Update the maximum height block node, and track the oldest block height
```java
maxHeightNode = genesisNode;
oldestBlockHeight = 1;
```

### 3. `getMaxHeightBlock` get the maximum height block
```java
public Block getMaxHeightBlock() {
    return maxHeightNode.block;
}
```
### 4. `getMaxHeightUTXOPool` get the UTXOPool for mining a new block on top of max height block
```java
public UTXOPool getMaxHeightUTXOPool() {
    // IMPLEMENT THIS
    return maxHeightNode.utxoPool;
}
```

### 5. `getTransactionPool` get the transaction pool to mine a new block
```java
public TransactionPool getTransactionPool() {
    // IMPLEMENT THIS
    return txPool;
}
```

### 6. `getOldestBlockHeight` get the height of oldest node
```java
public int getOldestBlockHeight() {
    return oldestBlockHeight;
}
```

### 7. `addBlock` add block to the block chain if it is valid
- Check whether the parent node is null
```java
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
```
- Check for validity of the block
```java
for (int i = 0; i < numberOfTx; i++) {
    txList[i] = block.getTransaction(i);
}
Transaction[] validTx = txHandler.handleTxs(txList);
if (validTx.length != numberOfTx) {
    System.out.println("Invalid transaction");
    return false;
}
```
- Check for block height
```java
if (parent.height + 1 <= maxHeightNode.height - CUT_OFF_AGE) {
    System.out.println("Invalid height");
    return false;
}
```
- Update transaction pool
```java
List<Transaction> transactions = block.getTransactions();
for (Transaction transaction : transactions) {
    txPool.removeTransaction(transaction.getHash());
}
```
- Update maxHeightNode, and keep around the most recent blocks

### 8. `addTransaction` add a transaction to the transaction pool
```java
public void addTransaction(Transaction tx) {
    txPool.addTransaction(tx);
}
```

## Test Case
### 1. `testValidTxs` test whether blockchain can identify invalid transactions
### 2. `testCorrectPrevBlockHash` test whether the previous block hash used for creating a new block is correct
### 3. `testEqualHash` test whether previous block hash value is in coincidence with PrevBlockHash of the current block
### 4. `testStorage` test whether the block storage is well-behaved, i.e., the number of blocks in the block chain should not exceed some specific number (e.g., 10)
