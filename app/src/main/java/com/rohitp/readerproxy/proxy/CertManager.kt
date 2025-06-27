package com.rohitp.readerproxy.proxy

import android.content.Context
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.InputStreamReader
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

class CertManager(ctx: Context) {

    private val caCert: X509Certificate
    private val caKey: PrivateKey
    private val cache = ConcurrentHashMap<String, SSLContext>()

    init {
        val p = PEMParser(InputStreamReader(ctx.assets.open("ca_cert.pem")))
        val caCertHolder = p.readObject() as X509CertificateHolder
        caCert = JcaX509CertificateConverter().getCertificate(caCertHolder)
        p.close()

        val pKey = PEMParser(InputStreamReader(ctx.assets.open("ca_key.pem")))
        caKey = JcaPEMKeyConverter().getPrivateKey((pKey.readObject() as PrivateKeyInfo))
        pKey.close()
    }

    /** SSLContext paired with a forged cert for [host]. */
    fun serverContext(host: String): SSLContext =
        cache.computeIfAbsent(host) { makeContextForHost(it) }

    private fun makeContextForHost(host: String): SSLContext {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()
        val now = Date()
        val cert = JcaX509v3CertificateBuilder(
            caCert, BigInteger.valueOf(System.nanoTime()),
            now, Date(now.time + 365L * 24 * 60 * 60 * 1000),
            X500Name("CN=$host"), keyPair.public
        ).apply {
            val san = GeneralNames(GeneralName(GeneralName.dNSName, host))
            addExtension(Extension.subjectAlternativeName, false, san)
        }.build(JcaContentSignerBuilder("SHA256withRSA").build(caKey))
            .let { JcaX509CertificateConverter().getCertificate(it) }

        val ks = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            setKeyEntry("key", keyPair.private, null, arrayOf(cert, caCert))
        }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(ks, null)
        }
        return SSLContext.getInstance("TLS").apply {
            init(kmf.keyManagers, null, null)
        }
    }
}
