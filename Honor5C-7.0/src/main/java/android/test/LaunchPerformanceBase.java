package android.test;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;

@Deprecated
public class LaunchPerformanceBase extends Instrumentation {
    public static final String LOG_TAG = "Launch Performance";
    protected Intent mIntent;
    protected Bundle mResults;

    public LaunchPerformanceBase() {
        this.mResults = new Bundle();
        this.mIntent = new Intent("android.intent.action.MAIN");
        this.mIntent.setFlags(268435456);
        setAutomaticPerformanceSnapshots();
    }

    protected void LaunchApp() {
        startActivitySync(this.mIntent);
        waitForIdleSync();
    }
}
