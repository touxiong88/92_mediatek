/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package org.bouncycastle.jce.provider.symmetric;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;

import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherKeyGenerator;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.engines.AESWrapEngine;
// BEGIN android-removed
// import org.bouncycastle.crypto.engines.RFC3211WrapEngine;
// import org.bouncycastle.crypto.macs.CMac;
// END android-removed
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.modes.OFBBlockCipher;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JCEBlockCipher;
import org.bouncycastle.jce.provider.JCEKeyGenerator;
import org.bouncycastle.jce.provider.JCEMac;
import org.bouncycastle.jce.provider.JDKAlgorithmParameterGenerator;
import org.bouncycastle.jce.provider.JDKAlgorithmParameters;
import org.bouncycastle.jce.provider.WrapCipherSpi;

public final class AES
{
    private AES()
    {
    }
    
    public static class ECB
        extends JCEBlockCipher
    {
        public ECB()
        {
            super(new AESFastEngine());
        }
    }

    // BEGIN android-removed
    // public static class CBC
    //    extends JCEBlockCipher
    // {
    //     public CBC()
    //     {
    //         super(new CBCBlockCipher(new AESFastEngine()), 128);
    //     }
    // }
    //
    // static public class CFB
    //     extends JCEBlockCipher
    // {
    //     public CFB()
    //     {
    //         super(new BufferedBlockCipher(new CFBBlockCipher(new AESFastEngine(), 128)), 128);
    //     }
    // }
    //
    // static public class OFB
    //     extends JCEBlockCipher
    // {
    //     public OFB()
    //     {
    //         super(new BufferedBlockCipher(new OFBBlockCipher(new AESFastEngine(), 128)), 128);
    //     }
    // }
    //
    // public static class AESCMAC
    //     extends JCEMac
    // {
    //     public AESCMAC()
    //     {
    //         super(new CMac(new AESFastEngine()));
    //     }
    // }
    // END android-removed

    static public class Wrap
        extends WrapCipherSpi
    {
        public Wrap()
        {
            super(new AESWrapEngine());
        }
    }

    // BEGIN android-removed
    // public static class RFC3211Wrap
    //     extends WrapCipherSpi
    // {
    //     public RFC3211Wrap()
    //     {
    //         super(new RFC3211WrapEngine(new AESFastEngine()), 16);
    //     }
    // }
    // END android-removed

    public static class KeyGen
        extends JCEKeyGenerator
    {
        public KeyGen()
        {
            this(192);
        }

        public KeyGen(int keySize)
        {
            super("AES", keySize, new CipherKeyGenerator());
        }
    }

    // BEGIN android-removed
    // public static class KeyGen128
    //     extends KeyGen
    // {
    //     public KeyGen128()
    //     {
    //         super(128);
    //     }
    // }
    //
    // public static class KeyGen192
    //     extends KeyGen
    // {
    //     public KeyGen192()
    //     {
    //         super(192);
    //     }
    // }
    //
    // public static class KeyGen256
    //     extends KeyGen
    // {
    //     public KeyGen256()
    //     {
    //         super(256);
    //     }
    // }
    //
    // public static class AlgParamGen
    //     extends JDKAlgorithmParameterGenerator
    // {
    //     protected void engineInit(
    //         AlgorithmParameterSpec genParamSpec,
    //         SecureRandom random)
    //         throws InvalidAlgorithmParameterException
    //     {
    //         throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for AES parameter generation.");
    //     }
    //
    //     protected AlgorithmParameters engineGenerateParameters()
    //     {
    //         byte[]  iv = new byte[16];
    //
    //         if (random == null)
    //         {
    //             random = new SecureRandom();
    //         }
    //
    //         random.nextBytes(iv);
    //
    //         AlgorithmParameters params;
    //
    //         try
    //         {
    //             params = AlgorithmParameters.getInstance("AES", BouncyCastleProvider.PROVIDER_NAME);
    //             params.init(new IvParameterSpec(iv));
    //         }
    //         catch (Exception e)
    //         {
    //             throw new RuntimeException(e.getMessage());
    //         }
    //
    //         return params;
    //     }
    // }
    // END android-removed

    public static class AlgParams
        extends JDKAlgorithmParameters.IVAlgorithmParameters
    {
        protected String engineToString()
        {
            return "AES IV";
        }
    }

    public static class Mappings
        extends HashMap
    {
        /**
         * These three got introduced in some messages as a result of a typo in an
         * early document. We don't produce anything using these OID values, but we'll
         * read them.
         */
        private static final String wrongAES128 = "2.16.840.1.101.3.4.2";
        private static final String wrongAES192 = "2.16.840.1.101.3.4.22";
        private static final String wrongAES256 = "2.16.840.1.101.3.4.42";

        public Mappings()
        {
            put("AlgorithmParameters.AES", "org.bouncycastle.jce.provider.symmetric.AES$AlgParams");
            put("Alg.Alias.AlgorithmParameters." + wrongAES128, "AES");
            put("Alg.Alias.AlgorithmParameters." + wrongAES192, "AES");
            put("Alg.Alias.AlgorithmParameters." + wrongAES256, "AES");
            put("Alg.Alias.AlgorithmParameters." + NISTObjectIdentifiers.id_aes128_CBC, "AES");
            put("Alg.Alias.AlgorithmParameters." + NISTObjectIdentifiers.id_aes192_CBC, "AES");
            put("Alg.Alias.AlgorithmParameters." + NISTObjectIdentifiers.id_aes256_CBC, "AES");

            // BEGIN android-removed
            // put("AlgorithmParameterGenerator.AES", "org.bouncycastle.jce.provider.symmetric.AES$AlgParamGen");
            // put("Alg.Alias.AlgorithmParameterGenerator." + wrongAES128, "AES");
            // put("Alg.Alias.AlgorithmParameterGenerator." + wrongAES192, "AES");
            // put("Alg.Alias.AlgorithmParameterGenerator." + wrongAES256, "AES");
            // put("Alg.Alias.AlgorithmParameterGenerator." + NISTObjectIdentifiers.id_aes128_CBC, "AES");
            // put("Alg.Alias.AlgorithmParameterGenerator." + NISTObjectIdentifiers.id_aes192_CBC, "AES");
            // put("Alg.Alias.AlgorithmParameterGenerator." + NISTObjectIdentifiers.id_aes256_CBC, "AES");
            // END android-removed

            put("Cipher.AES", "org.bouncycastle.jce.provider.symmetric.AES$ECB");
            put("Alg.Alias.Cipher." + wrongAES128, "AES");
            put("Alg.Alias.Cipher." + wrongAES192, "AES");
            put("Alg.Alias.Cipher." + wrongAES256, "AES");
            // BEGIN android-removed
            // put("Cipher." + NISTObjectIdentifiers.id_aes128_ECB, "org.bouncycastle.jce.provider.symmetric.AES$ECB");
            // put("Cipher." + NISTObjectIdentifiers.id_aes192_ECB, "org.bouncycastle.jce.provider.symmetric.AES$ECB");
            // put("Cipher." + NISTObjectIdentifiers.id_aes256_ECB, "org.bouncycastle.jce.provider.symmetric.AES$ECB");
            // put("Cipher." + NISTObjectIdentifiers.id_aes128_CBC, "org.bouncycastle.jce.provider.symmetric.AES$CBC");
            // put("Cipher." + NISTObjectIdentifiers.id_aes192_CBC, "org.bouncycastle.jce.provider.symmetric.AES$CBC");
            // put("Cipher." + NISTObjectIdentifiers.id_aes256_CBC, "org.bouncycastle.jce.provider.symmetric.AES$CBC");
            // put("Cipher." + NISTObjectIdentifiers.id_aes128_OFB, "org.bouncycastle.jce.provider.symmetric.AES$OFB");
            // put("Cipher." + NISTObjectIdentifiers.id_aes192_OFB, "org.bouncycastle.jce.provider.symmetric.AES$OFB");
            // put("Cipher." + NISTObjectIdentifiers.id_aes256_OFB, "org.bouncycastle.jce.provider.symmetric.AES$OFB");
            // put("Cipher." + NISTObjectIdentifiers.id_aes128_CFB, "org.bouncycastle.jce.provider.symmetric.AES$CFB");
            // put("Cipher." + NISTObjectIdentifiers.id_aes192_CFB, "org.bouncycastle.jce.provider.symmetric.AES$CFB");
            // put("Cipher." + NISTObjectIdentifiers.id_aes256_CFB, "org.bouncycastle.jce.provider.symmetric.AES$CFB");
            // END android-removed
            put("Cipher.AESWRAP", "org.bouncycastle.jce.provider.symmetric.AES$Wrap");
            put("Alg.Alias.Cipher." + NISTObjectIdentifiers.id_aes128_wrap, "AESWRAP");
            put("Alg.Alias.Cipher." + NISTObjectIdentifiers.id_aes192_wrap, "AESWRAP");
            put("Alg.Alias.Cipher." + NISTObjectIdentifiers.id_aes256_wrap, "AESWRAP");
            // BEGIN android-removed
            // put("Cipher.AESRFC3211WRAP", "org.bouncycastle.jce.provider.symmetric.AES$RFC3211Wrap");
            // END android-removed

            put("KeyGenerator.AES", "org.bouncycastle.jce.provider.symmetric.AES$KeyGen");
            // BEGIN android-removed
            // put("KeyGenerator.2.16.840.1.101.3.4.2", "org.bouncycastle.jce.provider.symmetric.AES$KeyGen128");
            // put("KeyGenerator.2.16.840.1.101.3.4.22", "org.bouncycastle.jce.provider.symmetric.AES$KeyGen192");
            // put("KeyGenerator.2.16.840.1.101.3.4.42", "org.bouncycastle.jce.provider.symmetric.AES$KeyGen256");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes128_ECB, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen128");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes128_CBC, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen128");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes128_OFB, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen128");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes128_CFB, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen128");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes192_ECB, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen192");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes192_CBC, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen192");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes192_OFB, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen192");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes192_CFB, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen192");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes256_ECB, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen256");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes256_CBC, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen256");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes256_OFB, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen256");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes256_CFB, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen256");
            // put("KeyGenerator.AESWRAP", "org.bouncycastle.jce.provider.symmetric.AES$KeyGen");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes128_wrap, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen128");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes192_wrap, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen192");
            // put("KeyGenerator." + NISTObjectIdentifiers.id_aes256_wrap, "org.bouncycastle.jce.provider.symmetric.AES$KeyGen256");
            //
            // put("Mac.AESCMAC", "org.bouncycastle.jce.provider.symmetric.AES$AESCMAC");
            // END android-removed
        }
    }
}
