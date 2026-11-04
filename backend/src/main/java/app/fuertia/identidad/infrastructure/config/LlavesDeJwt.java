package app.fuertia.identidad.infrastructure.config;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * El par de llaves con el que se firman (privada) y se verifican (pública) los JWT de acceso.
 *
 * ES256 = ECDSA con la curva P-256 y SHA-256. Es asimétrico: con la llave pública solo se puede
 * VERIFICAR, no firmar. Si mañana otro servicio necesita validar tokens, le damos la pública y no
 * la privada. (Con HS256 la misma clave sirve para las dos cosas.)
 *
 * La llave privada llega en FUERTIA_JWT_LLAVE_PRIVADA en formato PKCS8 PEM. La pública se calcula a
 * partir de ella (punto Q = d × G de la curva), así hay un solo secreto que administrar.
 */
public final class LlavesDeJwt {

    private static final Logger log = LoggerFactory.getLogger(LlavesDeJwt.class);
    private static final String CURVA = "secp256r1"; // el nombre técnico de P-256

    private final ECPrivateKey privada;
    private final ECPublicKey publica;

    private LlavesDeJwt(ECPrivateKey privada, ECPublicKey publica) {
        this.privada = privada;
        this.publica = publica;
    }

    /**
     * Decide de dónde salen las llaves.
     * - Con PEM: se usan esas.
     * - Sin PEM y en el perfil local: se genera un par temporal, con una advertencia. Los tokens
     *   dejan de servir al reiniciar y no sirven entre dos copias del backend, pero en local da igual.
     * - Sin PEM fuera del perfil local: la app no arranca. Mejor fallar al inicio que firmar con una
     *   llave que nadie conoce y que cambia en cada copia.
     */
    public static LlavesDeJwt cargar(String pem, boolean perfilLocal) {
        if (pem != null && !pem.isBlank()) {
            return desdePem(pem);
        }
        if (perfilLocal) {
            log.warn("No hay FUERTIA_JWT_LLAVE_PRIVADA: se usa una llave temporal generada al arrancar. "
                    + "Las sesiones se pierden al reiniciar. Solo sirve para desarrollo local.");
            return temporales();
        }
        throw new IllegalStateException("Falta la variable FUERTIA_JWT_LLAVE_PRIVADA (llave EC P-256 en PKCS8 PEM). "
                + "Sin ella no se pueden firmar los tokens de acceso.");
    }

    public static LlavesDeJwt desdePem(String pem) {
        try {
            // Las variables de entorno a veces traen los saltos de línea escritos como "\n".
            String limpio = pem.replace("\\n", "\n")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            if (limpio.isEmpty()) {
                throw new IllegalArgumentException("vacía");
            }
            byte[] der = Base64.getDecoder().decode(limpio);
            if (!(KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der)) instanceof ECPrivateKey privada)) {
                throw new IllegalArgumentException("no es una llave EC");
            }
            exigirP256(privada.getParams());
            return new LlavesDeJwt(privada, calcularPublica(privada));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // A propósito no se incluye el contenido de la llave en el mensaje.
            throw new IllegalStateException("FUERTIA_JWT_LLAVE_PRIVADA no es una llave EC P-256 válida en PKCS8 PEM ("
                    + e.getClass().getSimpleName() + ")", e);
        }
    }

    public static LlavesDeJwt temporales() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("EC");
            generador.initialize(new ECGenParameterSpec(CURVA));
            KeyPair par = generador.generateKeyPair();
            return new LlavesDeJwt((ECPrivateKey) par.getPrivate(), (ECPublicKey) par.getPublic());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo generar la llave temporal de JWT", e);
        }
    }

    public ECPrivateKey privada() {
        return privada;
    }

    public ECPublicKey publica() {
        return publica;
    }

    // ES256 exige P-256. Una llave P-384 o secp256k1 se parecería, pero el token no sería ES256.
    private static void exigirP256(ECParameterSpec parametros) throws GeneralSecurityException {
        ECParameterSpec p256 = parametrosP256();
        boolean esP256 = parametros.getOrder().equals(p256.getOrder())
                && parametros.getGenerator().equals(p256.getGenerator())
                && parametros.getCurve().equals(p256.getCurve());
        if (!esP256) {
            throw new IllegalArgumentException("la curva no es P-256");
        }
    }

    private static ECParameterSpec parametrosP256() throws GeneralSecurityException {
        AlgorithmParameters parametros = AlgorithmParameters.getInstance("EC");
        parametros.init(new ECGenParameterSpec(CURVA));
        return parametros.getParameterSpec(ECParameterSpec.class);
    }

    // Java no trae una función para sacar la pública de la privada; Bouncy Castle (que ya usamos
    // para Argon2) sí sabe multiplicar puntos de la curva.
    private static ECPublicKey calcularPublica(ECPrivateKey privada) throws GeneralSecurityException {
        ECNamedCurveParameterSpec curva = ECNamedCurveTable.getParameterSpec(CURVA);
        org.bouncycastle.math.ec.ECPoint q = curva.getG().multiply(privada.getS()).normalize();
        BigInteger x = q.getAffineXCoord().toBigInteger();
        BigInteger y = q.getAffineYCoord().toBigInteger();
        return (ECPublicKey) KeyFactory.getInstance("EC")
                .generatePublic(new ECPublicKeySpec(new ECPoint(x, y), privada.getParams()));
    }

    @Override
    public String toString() {
        return "LlavesDeJwt[ES256, privada=****]";
    }
}
