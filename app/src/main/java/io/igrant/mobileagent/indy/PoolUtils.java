package io.igrant.mobileagent.indy;

import org.apache.commons.io.FileUtils;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.pool.PoolJSONParameters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class PoolUtils {

//    private static final String DEFAULT_POOL_NAME = "default";
    public static final int TEST_TIMEOUT_FOR_REQUEST_ENSURE = 200_000;
    private static final int RESUBMIT_REQUEST_TIMEOUT = 5_000;
    private static final int RESUBMIT_REQUEST_CNT = 3;

    public static File createGenesisTxnFile(String filename,int type) throws IOException {
        String path = EnvironmentUtils.getTmpPath(filename);

        File file = new File(path);

		FileUtils.forceMkdirParent(file);

        writeTransactions(file,type);
        return file;
    }

    public static void writeTransactions(File file,int type) throws IOException {
        String testPoolIp = EnvironmentUtils.getTestPoolIP();

        // this data and pool_transactions_genesis must have the same data and IP addresses
        ArrayList<String> defaultTxns = LedgerNetworkType.getTransaction(type);

        FileWriter fw = new FileWriter(file);
        for (String defaultTxn : defaultTxns) {
            fw.write(defaultTxn);
            fw.write("\n");
        }

        fw.close();

    }

    public static String createPoolLedgerConfig(int type) throws InterruptedException, ExecutionException, IndyException, IOException {
        Pool.deletePoolLedgerConfig(LedgerNetworkType.getConfigName());
        createPoolLedgerConfig(LedgerNetworkType.getConfigName(),type);
        return LedgerNetworkType.getConfigName();
    }

    private static void createPoolLedgerConfig(String poolName,int type) throws IOException {
        File genesisTxnFile = createGenesisTxnFile(LedgerNetworkType.getFileName(),type);
        PoolJSONParameters.CreatePoolLedgerConfigJSONParameter createPoolLedgerConfigJSONParameter
                = new PoolJSONParameters.CreatePoolLedgerConfigJSONParameter(genesisTxnFile.getAbsolutePath());
        try {
            Pool.createPoolLedgerConfig(poolName, createPoolLedgerConfigJSONParameter.toJson()).get();
        } catch (ExecutionException | InterruptedException | IndyException e) {
            e.printStackTrace();
        }
    }

    public static Pool createAndOpenPoolLedger(int type) throws IndyException, InterruptedException, ExecutionException, IOException {
        String poolName = PoolUtils.createPoolLedgerConfig(type);

        PoolJSONParameters.OpenPoolLedgerJSONParameter config = new PoolJSONParameters.OpenPoolLedgerJSONParameter(null, null);
        Pool.setProtocolVersion(2);
        return Pool.openPoolLedger(poolName, config.toJson()).get();
    }

    public interface PoolResponseChecker {
        boolean check(String response);
    }

    public interface ActionChecker {
        String action() throws IndyException, ExecutionException, InterruptedException;
    }

    public static String ensurePreviousRequestApplied(Pool pool, String checkerRequest, PoolResponseChecker checker) throws IndyException, ExecutionException, InterruptedException {
        for (int i = 0; i < RESUBMIT_REQUEST_CNT; i++) {
            String response = Ledger.submitRequest(pool, checkerRequest).get();
            if (checker.check(response)) {
                return response;
            }
            Thread.sleep(RESUBMIT_REQUEST_TIMEOUT);
        }
        throw new IllegalStateException();
    }

    public static boolean retryCheck(ActionChecker action, PoolResponseChecker checker) throws InterruptedException, ExecutionException, IndyException {
        for (int i = 0; i < RESUBMIT_REQUEST_CNT; i++) {
            if (checker.check(action.action())) {
                return true;
            }
        }
        return false;
    }
}