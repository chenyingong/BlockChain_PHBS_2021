import java.security.PublicKey;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;


public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

        // Use set to avoid duplicated elements
        HashSet<UTXO> utxoSet = new HashSet<UTXO>();
        double totalInValue = 0.0;
        double totalOutValue = 0.0;

        int inputSize = tx.getInputs().size();
        for (int i = 0; i < inputSize; i++) {
            Transaction.Input txIn = tx.getInput(i);
            UTXO utxoCheck = new UTXO(txIn.prevTxHash, txIn.outputIndex);

            // Check (1)
            if (!this.utxoPool.contains(utxoCheck)) {
                System.out.println("The output is not in the current UTXO pool.");
                return false;
            }

            // Check (2)
            Transaction.Output txOut = this.utxoPool.getTxOutput(utxoCheck);
            // Check if the output is in the current UTXO pool
            if (txOut == null) {
                System.out.println("The output is not in the current UTXO pool.");
                return false;
            }
            PublicKey pk = txOut.address;
            if (!Crypto.verifySignature(pk, tx.getRawDataToSign(i), txIn.signature)) {
                System.out.println("The signature is invalid.");
                return false;
            }

            // Check (3)
            if (utxoSet.contains(utxoCheck)) {
                System.out.println("Multiple-time claimed.");
                return false;
            }
            else {
                utxoSet.add(utxoCheck);
            }

            // Update value
            totalInValue += txOut.value;
        }

        // Check (4)
        int outputSize = tx.getOutputs().size();
        for (int i = 0; i < outputSize; i++) {
            Transaction.Output txOut = tx.getOutput(i);
            double outValue = txOut.value;
            if (outValue < 0.0) {
                System.out.println("Output value is negative.");
                return false;
            }
            totalOutValue += outValue;
        }

        // Check (5)
        if (totalInValue < totalOutValue) {
            System.out.println("Input values are less than the sum of its output values.");
            return false;
        }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {

        // List of all possible txs
        ArrayList<Transaction> possibleTxsList =  new ArrayList<Transaction>(Arrays.asList(possibleTxs));
        // List of accepted txs
        ArrayList<Transaction> acceptedList = new ArrayList<Transaction>();

        boolean flag = true;

        while (flag) {
            int numTxs = possibleTxsList.size();  // current number of transactions
            // List of accepted txs going to remove from possibleTxsList
            ArrayList<Transaction> txToRemove = new ArrayList<Transaction>();
            for (Transaction tx: possibleTxsList) {
                if (isValidTx(tx)) {
                    acceptedList.add(tx);
                    txToRemove.add(tx);

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
                }
            }
            // Remove all accepted txs
            possibleTxsList.removeAll(txToRemove);
            // Check if there is any tx in possibleTxsList
            int newNumTxs = possibleTxsList.size();
            flag = newNumTxs < numTxs && newNumTxs != 0;
        }
        return acceptedList.toArray(new Transaction[acceptedList.size()]);
    }

}
