package javax.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import sun.security.jca.GetInstance;
import sun.security.jca.GetInstance.Instance;
import sun.security.util.Debug;

public class KeyAgreement {
    private static final int I_NO_PARAMS = 1;
    private static final int I_PARAMS = 2;
    private static final Debug debug = null;
    private static int warnCount;
    private final String algorithm;
    private final Object lock;
    private Provider provider;
    private KeyAgreementSpi spi;

    static {
        /* JADX: method processing error */
/*
        Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: javax.crypto.KeyAgreement.<clinit>():void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:113)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:256)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:281)
	at jadx.api.JavaClass.decompile(JavaClass.java:59)
	at jadx.api.JadxDecompiler$1.run(JadxDecompiler.java:161)
Caused by: jadx.core.utils.exceptions.DecodeException:  in method: javax.crypto.KeyAgreement.<clinit>():void
	at jadx.core.dex.instructions.InsnDecoder.decodeInsns(InsnDecoder.java:46)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:98)
	... 5 more
Caused by: java.lang.IllegalArgumentException: bogus opcode: 0073
	at com.android.dx.io.OpcodeInfo.get(OpcodeInfo.java:1197)
	at com.android.dx.io.OpcodeInfo.getFormat(OpcodeInfo.java:1212)
	at com.android.dx.io.instructions.DecodedInstruction.decode(DecodedInstruction.java:72)
	at jadx.core.dex.instructions.InsnDecoder.decodeInsns(InsnDecoder.java:43)
	... 6 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: javax.crypto.KeyAgreement.<clinit>():void");
    }

    protected KeyAgreement(KeyAgreementSpi keyAgreeSpi, Provider provider, String algorithm) {
        this.spi = keyAgreeSpi;
        this.provider = provider;
        this.algorithm = algorithm;
        this.lock = null;
    }

    private KeyAgreement(String algorithm) {
        this.algorithm = algorithm;
        this.lock = new Object();
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public static final KeyAgreement getInstance(String algorithm) throws NoSuchAlgorithmException {
        for (Service s : GetInstance.getServices("KeyAgreement", algorithm)) {
            if (JceSecurity.canUseProvider(s.getProvider())) {
                return new KeyAgreement(algorithm);
            }
        }
        throw new NoSuchAlgorithmException("Algorithm " + algorithm + " not available");
    }

    public static final KeyAgreement getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        Instance instance = JceSecurity.getInstance("KeyAgreement", KeyAgreementSpi.class, algorithm, provider);
        return new KeyAgreement((KeyAgreementSpi) instance.impl, instance.provider, algorithm);
    }

    public static final KeyAgreement getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        Instance instance = JceSecurity.getInstance("KeyAgreement", KeyAgreementSpi.class, algorithm, provider);
        return new KeyAgreement((KeyAgreementSpi) instance.impl, instance.provider, algorithm);
    }

    void chooseFirstProvider() {
        if (this.spi == null) {
            synchronized (this.lock) {
                if (this.spi != null) {
                    return;
                }
                if (debug != null) {
                    int w = warnCount - 1;
                    warnCount = w;
                    if (w >= 0) {
                        debug.println("KeyAgreement.init() not first method called, disabling delayed provider selection");
                        if (w == 0) {
                            debug.println("Further warnings of this type will be suppressed");
                        }
                        new Exception("Call trace").printStackTrace();
                    }
                }
                Throwable lastException = null;
                for (Service s : GetInstance.getServices("KeyAgreement", this.algorithm)) {
                    if (JceSecurity.canUseProvider(s.getProvider())) {
                        try {
                            Object obj = s.newInstance(null);
                            if (obj instanceof KeyAgreementSpi) {
                                this.spi = (KeyAgreementSpi) obj;
                                this.provider = s.getProvider();
                                return;
                            }
                        } catch (Throwable e) {
                            lastException = e;
                        }
                    }
                }
                ProviderException e2 = new ProviderException("Could not construct KeyAgreementSpi instance");
                if (lastException != null) {
                    e2.initCause(lastException);
                }
                throw e2;
            }
        }
    }

    private void implInit(KeyAgreementSpi spi, int type, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (type == I_NO_PARAMS) {
            spi.engineInit(key, random);
        } else {
            spi.engineInit(key, params, random);
        }
    }

    private void chooseProvider(int initType, Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        synchronized (this.lock) {
            if (this.spi == null || key != null) {
                Throwable lastException = null;
                for (Service s : GetInstance.getServices("KeyAgreement", this.algorithm)) {
                    if (s.supportsParameter(key) && JceSecurity.canUseProvider(s.getProvider())) {
                        try {
                            KeyAgreementSpi spi = (KeyAgreementSpi) s.newInstance(null);
                            implInit(spi, initType, key, params, random);
                            this.provider = s.getProvider();
                            this.spi = spi;
                            return;
                        } catch (Throwable e) {
                            if (lastException == null) {
                                lastException = e;
                            }
                        }
                    }
                }
                if (lastException instanceof InvalidKeyException) {
                    throw ((InvalidKeyException) lastException);
                } else if (lastException instanceof InvalidAlgorithmParameterException) {
                    throw ((InvalidAlgorithmParameterException) lastException);
                } else if (lastException instanceof RuntimeException) {
                    throw ((RuntimeException) lastException);
                } else {
                    throw new InvalidKeyException("No installed provider supports this key: " + (key != null ? key.getClass().getName() : "(null)"), lastException);
                }
            }
            implInit(this.spi, initType, key, params, random);
        }
    }

    public final Provider getProvider() {
        chooseFirstProvider();
        return this.provider;
    }

    public final void init(Key key) throws InvalidKeyException {
        init(key, JceSecurity.RANDOM);
    }

    public final void init(Key key, SecureRandom random) throws InvalidKeyException {
        if (this.spi == null || !(key == null || this.lock == null)) {
            try {
                chooseProvider(I_NO_PARAMS, key, null, random);
                return;
            } catch (Throwable e) {
                throw new InvalidKeyException(e);
            }
        }
        this.spi.engineInit(key, random);
    }

    public final void init(Key key, AlgorithmParameterSpec params) throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(key, params, JceSecurity.RANDOM);
    }

    public final void init(Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (this.spi != null) {
            this.spi.engineInit(key, params, random);
        } else {
            chooseProvider(I_PARAMS, key, params, random);
        }
    }

    public final Key doPhase(Key key, boolean lastPhase) throws InvalidKeyException, IllegalStateException {
        chooseFirstProvider();
        return this.spi.engineDoPhase(key, lastPhase);
    }

    public final byte[] generateSecret() throws IllegalStateException {
        chooseFirstProvider();
        return this.spi.engineGenerateSecret();
    }

    public final int generateSecret(byte[] sharedSecret, int offset) throws IllegalStateException, ShortBufferException {
        chooseFirstProvider();
        return this.spi.engineGenerateSecret(sharedSecret, offset);
    }

    public final SecretKey generateSecret(String algorithm) throws IllegalStateException, NoSuchAlgorithmException, InvalidKeyException {
        chooseFirstProvider();
        return this.spi.engineGenerateSecret(algorithm);
    }
}
