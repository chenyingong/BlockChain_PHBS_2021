# Block Chain and Digital Currency, Homework #1
Chenyin Gong
1901212480
2021-06-09

## Summary
### 1. `Constructor` make a copy of UTXO Pool by using the UTXOPool(UTXOPool uPool) method
```java
private UTXOPool utxoPool;
public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }
```

### 2. `isValidTx` check whether the transaction is valid
- Whether the UTXO is in the pool
```java
if (!this.utxoPool.contains(utxoCheck)) {
    System.out.println("The output is not in the current UTXO pool.");
    return false;
}
```
- Whether the signature is valid
```java
            if (!Crypto.verifySignature(pk, tx.getRawDataToSign(i), txIn.signature)) {
                System.out.println("The signature is invalid.");
                return false;
            }
```
- Whether the UTXO is claimed multiple times
```java
            if (utxoSet.contains(utxoCheck)) {
                System.out.println("Multiple-time claimed.");
                return false;
            }
```
- Whether the output value is negative
```java
        for (int i = 0; i < outputSize; i++) {
            Transaction.Output txOut = tx.getOutput(i);
            double outValue = txOut.value;
            if (outValue < 0.0) {
                System.out.println("Output value is negative.");
                return false;
            }
            totalOutValue += outValue;
        }
```
- Whether input values are less than the sum of output values
```java
        if (totalInValue < totalOutValue) {
            System.out.println("Input values are less than the sum of its output values.");
            return false;
        }
```

### 3. `handleTxs` check an unordered array of proposed transactions
- For each transaction, check whether it is valid by `isValidTx`
- Update UTXO pool
```java
                    // Delete inputs
                    for (Transaction.Input txIn : tx.getInputs()) {
                        UTXO utxo = new UTXO(txIn.prevTxHash, txIn.outputIndex);
                        utxoPool.removeUTXO(utxo);
                    }

                    // Add outputs
                    int index = 0;
                    for (Transaction.Output txOut : tx.getOutputs()) {
                        UTXO utxo = new UTXO(tx.getHash(), index);
                        this.utxoPool.addUTXO(utxo, txOut);
                        index++;
                    }
```
- Keep tracking a list of accepted transactions
```java
                    acceptedList.add(tx);
                    txToRemove.add(tx);
```
- Accepted transactions will be removed from initial list
```java
            possibleTxsList.removeAll(txToRemove);
```
- If there is no transaction to check, return accepted transactions

## Test Case
### 1. Test for `TxHandler.isValidTx`
- `testOutputsInUTXOPool` test if all outputs claimed by transactions are in the current UTXO pool. The output of initial UTXO is NOT included in the UTXO pool
- `testValidSig` test the signatures on each input of transactions are valid. Use a incorrect signature for the transaction
- `testNoDoubleSpending` test for double spending. Use the same input for a transaction
- `testValidOutputValues` test for non-negative output values. Transfer negative value of coins
- `testValidBalance` test for if the sum of input values is greater than or equal to the sum of its output. Output values are greater than input value
### 2. Test for `TxHandler.HandleTxs`
- Make three transactions `tx1`, `tx2`, and `tx3`, where only `tx3` is an invalid transaction. Assert the statement that the number of accepted transactions is two.
