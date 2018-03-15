package zs.live.utils;

import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Map;

/**
 * AES 加/解密工具类
 *
 *
 */
public class CipherUtil {

	private static final char[] DIGITS_LOWER = new char[] { '0', '1', '2', '3',
			'4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	private static final char[] DIGITS_UPPER = new char[] { '0', '1', '2', '3',
			'4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    final static String AES_KEY="LiVer%^&rEcoRD%^";

    final static String AES_KEY_H5_TEST="LiVh5%^&rEcoRD%^";
    final static String AES_KEY_H5_RES_TEST="LiVh5%^&rEcoRD%1";
    final static String AES_KEY_APP_TEST="LiVer%^&rEcoRD%^";
    final static String AES_KEY_APP_RES_TEST="LiVer%^&rEcoRD%1";

    final static String AES_KEY_H5="30a83908bf8044b082c7e8012576dc2f";
    final static String AES_KEY_H5_RES="57c93c96b68742d4a0d2b459912f77b7";
    final static String AES_KEY_APP="e3515d4456244ac6b70644ba3a0dcd50";
    final static String AES_KEY_APP_RES="88f4e2dc580447c784d73b5342b672f5";

	/**
	 * 转化十六进制格式的字符串转化为字节数组
	 *
	 * @param s
	 *            格式如"8ADF5438EE2358912D96FD6822DF1090"
	 * @return s对应的字节数组
	 */
	public static byte[] hex2byte(String s) {
		if (s == null || s.length() == 0)
			return new byte[0];
		if (s.length() == 1)
			s = "0" + s;
		byte[] baKeyword = new byte[s.length() / 2];
		for (int i = 0; i < baKeyword.length; i++) {
			try {
				baKeyword[i] = (byte) (0xff & Integer.parseInt(
						s.substring(i * 2, i * 2 + 2), 16));
			} catch (Exception e) {
				System.err.println(e);
			}
		}

		return baKeyword;
	}

	/**
	 * 字节数组转化为对应的十六进制的字符串
	 *
	 * @param data
	 * @return
	 */
    public static String byte2hex(byte[] data) {
		if (data == null)
			return "";
		else
			return encodeHexString(data).toUpperCase();
	}

	public static String encodeHexString(byte[] data) {
		return new String(encodeHex(data, true));
	}

	public static char[] encodeHex(byte[] data, boolean toLowerCase) {
		return encodeHex(data, toLowerCase ? DIGITS_LOWER : DIGITS_UPPER);
	}

	private static char[] encodeHex(byte[] data, char[] toDigits) {
		int l = data.length;
		char[] out = new char[l << 1];
		int i = 0;

		for (int j = 0; i < l; ++i) {
			out[j++] = toDigits[(240 & data[i]) >>> 4];
			out[j++] = toDigits[15 & data[i]];
		}

		return out;
	}

	/**
	 * 转换密钥
	 *
	 * @param key
	 *            二进制密钥
	 * @return Key 密钥
	 * */
	private static Key toKey(byte[] key, String key_algorithm) throws Exception {
		// 实例化DES密钥
		// 生成密钥
		SecretKey secretKey = new SecretKeySpec(key, key_algorithm);
		return secretKey;
	}

	/**
	 * 加密数据
	 *
	 * @param data
	 *            待加密数据
	 * @param key
	 *            加密密钥
	 * @param key_algorithm
	 *            密钥算法
	 * @param cipher_algorithm
	 *            数据加密算法
	 * @return 加密后数据
	 * @throws Exception
	 */
	private static byte[] encrypt(byte[] data, byte[] key,
			String key_algorithm, String cipher_algorithm,
			AlgorithmParameterSpec params) throws Exception {
		Key k = toKey(key, key_algorithm);
		Cipher cipher = Cipher.getInstance(cipher_algorithm);
		// 初始化，设置为加密模式
		if (params == null)
			cipher.init(Cipher.ENCRYPT_MODE, k);
		else
			cipher.init(Cipher.ENCRYPT_MODE, k, params);
		// 执行操作
		return cipher.doFinal(data);
	}

	/**
	 * 解密数据
	 *
	 * @param data
	 *            待解密数据
	 * @param key
	 *            解密密钥
	 * @param key_algorithm
	 *            密钥算法
	 * @param cipher_algorithm
	 *            数据解密算法
	 * @return 解密后数据
	 * @throws Exception
	 */
	private static byte[] decrypt(byte[] data, byte[] key,
			String key_algorithm, String cipher_algorithm,
			AlgorithmParameterSpec params) throws Exception {
		Key k = toKey(key, key_algorithm);
		Cipher cipher = Cipher.getInstance(cipher_algorithm);
		// 初始化，设置为解密模式
		if (params == null)
			cipher.init(Cipher.DECRYPT_MODE, k);
		else
			cipher.init(Cipher.DECRYPT_MODE, k, params);
		// 执行操作
		return cipher.doFinal(data);
	}

	/**
	 * 加密数据 采用aes ofb模式
	 *
	 * @param content
	 *            带加密数据
	 * @param aesKey
	 *            加密密钥
	 * @return 加密后数据
	 * @throws Exception
	 */
	public static String encryptAES(String content,String aesKey) throws Exception {
//		byte[] iv = { 'a', 'a', 'c', 'd', 'e', 'f', '0', '1', '2', '3', '4',
//				'5', '6', '7', '8', '9' };
//		随机产生16位的iv
		byte[] iv = new byte[16];
		SecureRandom random = new SecureRandom();
		random.nextBytes(iv);
		javax.crypto.spec.IvParameterSpec params = new javax.crypto.spec.IvParameterSpec(
				iv);
        byte[] encryData = encrypt(content.getBytes(), aesKey.getBytes(), "AES", "AES/OFB128/NoPadding", params);
//        return byte2hex(iv)+byte2hex(encryData);
        return byte2hex(iv)+new BASE64Encoder().encode(encryData).replaceAll("\\+","%2B");
	}

	/**
	 * 解密数据 采用aes ofb模式
	 *
	 * @param data
	 * 待解密数据
     *@param iv
     * 偏移量
	 * @return 解密后数据
	 * @throws Exception
	 */
	public static byte[] decryptAES(byte[] data,byte[] iv,String aesKey) throws Exception {
		javax.crypto.spec.IvParameterSpec params = new javax.crypto.spec.IvParameterSpec(iv);
		return decrypt(data, aesKey.getBytes("utf-8"), "AES", "AES/OFB128/NoPadding", params);
	}
    public  static String decryptAES(String content){
        String decryptStr="";
        try{
            String tmpIV=content.substring(0,32);
            content=content.substring(32);
            decryptStr=new String(decryptAES(hex2byte(content),hex2byte(tmpIV),AES_KEY), "utf-8");
        }catch (Exception e){
            e.printStackTrace();
        }
        return  decryptStr;
    }

    public  static String decryptAESNew(String content,String aesKey){
        String decryptStr="";
        try{
            String tmpIV=content.substring(0,32);
            content=content.substring(32);
            content = content.replaceAll("%2B","+").replaceAll("\\\\r", "").replaceAll("\\\\n","");
            decryptStr=new String(decryptAES(new BASE64Decoder().decodeBuffer(content),hex2byte(tmpIV),aesKey), "utf-8");
//            decryptStr=new String(decryptAES(hex2byte(content),hex2byte(tmpIV),aesKey), "utf-8");
        }catch (Exception e){
            e.printStackTrace();
        }
        return  decryptStr;
    }

    public static byte[] genIV() {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    public static String getAesNormalKey(String env,String isFrom,String type){
        String normalKey = "";
        String key ="";
        if("encrypt".equals(type)){//获取加密的固定key
            if("h5".equals(isFrom)){//h5的请求
                normalKey = "test".equals(env) ? AES_KEY_H5_RES_TEST : AES_KEY_H5_RES;
            }else{
                normalKey = "test".equals(env) ? AES_KEY_APP_RES_TEST : AES_KEY_APP_RES;
            }
        }else{//获取解密的固定key
            if("h5".equals(isFrom)){//h5的请求
                normalKey = "test".equals(env) ? AES_KEY_H5_TEST : AES_KEY_H5;
            }else{
                normalKey = "test".equals(env) ? AES_KEY_APP_TEST : AES_KEY_APP;
            }
        }
        return normalKey;
    }

    public static String getAesKey(String env,String isFrom,String type,String userIdSha1,String uuid){
        String aesKey = "";
        String normalKey = getAesNormalKey(env,isFrom,type);
        String md5 = Md5Util.md5(normalKey+userIdSha1+uuid);
        aesKey = md5.substring(16, md5.length());
        System.out.println("getAesKey params,aesKey="+aesKey+",env="+env+",normalKey="+normalKey+",userIdSha1="+userIdSha1+",uuId="+uuid);
        return aesKey;
    }

    public static void main(String a[]) throws Exception{
//        Map host = new HashedMap();
//        host.put("name","廖静");
//        host.put("age","liaojing");
        Map map= new HashedMap();
        map.put("psize",10);
        map.put("vc","5.6.3");
        map.put("appId","廖静是好人，liaojing is a nice girl");
        map.put("appModel","HM NOTE 1TD");
        map.put("token","f3b33aa8-1473-4ca4-861b-62c5b6527d88");
        map.put("userId","1004632435");
        map.put("categoryId",0);

//        map.put("isForce",0);
//        map.put("userId",1455042);
//        map.put("vc","AES高级加密标准，在密码学中又称Rijndael加密法，是美国联邦政府采用的一种区块加密标准。这个标准用来替代原先的DES，已经被多方分析且广为全世界所使用。经过五年的甄选流程，高级加密标准由美国国家标准与技术研究院（NIST）于2001年11月26日发布于FIPS PUB 197，并在2002年5月26日成为有效的标准。2006年，高级加密标准已然成为对称密钥加密中最流行的算法之一。本软件是用java语言开发，实现了AES算法对文件的加密和解密，并在界面上加了进度条，来提示用户加密解密的进度。如果不足之处，欢迎留言。");
//        map.put("title","AES高级加密标准，在密码学中又称Rijndael加密法，是美国联邦政府采用的一种区块加密标准。这个标准用来替代原先的DES，已经被多方分析且广为全世界所使用。经过五年的甄选流程，高级加密标准由美国国家标准与技术研究院（NIST）于2001年11月26日发布于FIPS PUB 197，并在2002年5月26日成为有效的标准。2006年，高级加密标准已然成为对称密钥加密中最流行的算法之一。本软件是用java语言开发，实现了AES算法对文件的加密和解密，并在界面上加了进度条，来提示用户加密解密的进度。如果不足之处，欢迎留言。");
        String data = JSON.toJSONString(map);
        System.out.println(data);
        String key = AES_KEY_APP+"828f720439cefaeb"+"777a65b9476e4ed39959d1c4d1335e4a";
        String md5Key = Md5Util.md5(key);
        key= md5Key.substring(16, md5Key.length());
//        key  = "04fded85865a1192";
        System.err.println("aesKey:"+key);
		String enData = CipherUtil.encryptAES(data, key);
		System.out.println("加密数据:" + enData);
        String deData = CipherUtil.decryptAESNew(enData,key);
        System.out.println("解密数据:" + deData);

//        String test = "4d5830316f3331614f76466871524879b00163efbf82c4ec0044f2a6ef71b106f9ad265d6cb9549644258599c832290b9822fd9eb89e5d9202f218b7b2d753765b24a9971099ba4bedfe66276f42feff9c2413d3f002939462f7958920786767b26eb8a47996557b4b3644cc9489ea07af7bc566716f54af47a78b72eab8f436c9ac7b9fdddce4ee9557aed7c558ce2068932a5cd6ddcedfdf08e0d8c17728bf0f380a6ab605ecdbf3fffdd946428f0c0241d3b0ef5d86fb4828425644da9980215cafa3f5436456428cd03d21db37d49497fedeab21e7f516da242b6d434fb60f115cbdec";
//        String normalKey = CipherUtil.getAesNormalKey(0,"decrypt");
////        String testKey = CipherUtil.getAesKey(0, "decrypt",1000007722L,"F52528DC8DEC4DB283EA47C69AD3E5F3");
////        System.out.println(testKey);
////        System.out.println(CipherUtil.encryptAES(data,testKey));
     }
}
