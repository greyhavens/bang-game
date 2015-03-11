//
// $Id$

package com.threerings.bang.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import com.google.common.collect.Sets;

import com.samskivert.net.MACUtil;
import com.samskivert.util.RunAnywhere;
import com.samskivert.util.StringUtil;

import static com.threerings.bang.Log.log;

/**
 * Try and nail down the identity of the user based on MAC address and secret
 * file machinations.
 */
public class IdentUtil
{
    /**
     * An identifier used to indicate to the server that we are having
     * trouble generating our own ident.
     */
    public static String NO_IDENT = "ZHNmYWtsd2VxZmprbGpvZGxrc2pmb2lq";

    /**
     * Acquires and returns the machine identifier for this machine.
     */
    public static String getMachineIdentifier ()
    {
        // first try reading the secret file
        String ident = readSecretFile();
        if (!StringUtil.isBlank(ident)) {
            return ident;
        }

        try {
            // try making an ident out of their MAC addresses
            String[] macs = MACUtil.getMACAddresses();
            if (macs.length > 0) {
                String allMacs = StringUtil.join(macs);
                ident = StringUtil.md5hex(allMacs);

                // BEGIN: ENCODEIDENT
                StringBuilder encoded = new StringBuilder();
                byte[] identArray = decodeHex(ident);
                byte[] checksum = computeChecksum(identArray);
                for (int ii = 0; ii < identArray.length; ii++) {
                    byte value = (byte)((identArray[ii] * 4) + checksum[ii]);
                    value = (byte)((ENCRYPTION_MULTIPLIER * value) % 64);
                    encoded.append((char)BASE_64_TO_CHAR[value]);
                }
                ident = encoded.toString();
                // END: ENCODEIDENT

                // make sure we write it out so we can just read our file in
                // the future and not cause freakout if users are running
                // monitoring software that gets pissy when we try to read the
                // MAC
                writeSecretFile(ident);
                return ident;
            }

        } catch (Exception e) {
            log.warning("Failed creating ident.", e);
        }

        // Golly, who'd have expected -that- to fail?
        return NO_IDENT;
    }

    /**
     * Allows for command line conversion of decoded idents stored in the
     * database to encoded idents for updating the non-unique idents array.
     */
    public static void main (String[] args)
    {
        String decoded = args[0];
        if (decoded.length() == 33 && decoded.startsWith("C")) {
            decoded = decoded.substring(1);
        }
        if (decoded.length() != 32) {
            throw new IllegalArgumentException("Incorrect ident length");
        }
        System.out.println(IdentUtil.encodeIdent(decoded));
    }

    /**
     * Produces an obfuscated base-64 encoded ident with checksum data from a
     * hex string MD5-style machine ident.
     *
     * <p> Note: This function will be considered "dead code" from the
     * perspective of Proguard client wrapping, as it is spread out throughout
     * the client code to prevent reverse engineering.
     */
    public static String encodeIdent (String ident)
    {
        StringBuilder encoded = new StringBuilder();
        byte[] identArray = decodeHex(ident);
        byte[] checksum = computeChecksum(identArray);
        for (int ii = 0; ii < identArray.length; ii++) {
            byte value = (byte)((identArray[ii] * 4) + checksum[ii]);
            value = (byte)((ENCRYPTION_MULTIPLIER * value) % 64);
            encoded.append((char)BASE_64_TO_CHAR[value]);
        }
        return encoded.toString();
    }

    /**
     * Decodes an obfuscated base-64 encoded ident with checksum data,
     * producing the original hex string MD5-style machine ident.
     *
     * @throws Exception if the checksum data is invalid.
     */
    public static String decodeIdent (String encoded)
        throws Exception
    {
        if (encoded == null) {
            return null;
        }

        // sanity check
        encoded = StringUtil.sanitize(encoded, "[A-Za-z0-9/+]");
        if (encoded.length() != 32) {
            throw new Exception(
                "Invalid characters or length [encoded=" + encoded + "].");
        }

        StringBuilder decoded = new StringBuilder();
        byte[] checksum = new byte[32];
        char[] characters = encoded.toCharArray();
        for (int ii = 0; ii < characters.length; ii++) {
            byte value = CHAR_TO_BASE_64[characters[ii]];
            value = (byte)((DECRYPTION_MULTIPLIER * value) % 64);
            checksum[ii] = (byte)(value % 4);
            decoded.append(Integer.toHexString(value / 4));
        }

        String ident = decoded.toString();
        String got = new String(checksum);
        String expected = new String(computeChecksum(decodeHex(ident)));
        if (!expected.equals(got)) {
            throw new Exception("Checksum failed [ident=" + ident +
                                ", encoded=" + encoded + "].");
        }
        return ident;
    }

    /**
     * Computes a checksum value for a 32-digit hex sequence.
     */
    protected static byte[] computeChecksum (byte[] ident)
    {
        byte[] checksum = new byte[32];
        for (int ii = 0; ii < 16; ii++) {
            // take only the bottom 4 bytes of the xoring of each character
            // with its counterpart in the other half of the checksum
            int value = ident[ii] ^ ident[ii + 16];
            checksum[2 * ii] = (byte)(value & 3);
            checksum[2 * ii + 1] = (byte)((value >> 2) & 3);
        }
        return checksum;
    }

    /**
     * Massages a string containing a hex sequence into an array of bytes
     * containing the hex value of each character.
     */
    protected static byte[] decodeHex (String ident)
    {
        char[] characters = ident.toCharArray();
        byte[] values = new byte[characters.length];
        for (int ii = 0; ii < characters.length; ii++) {
            values[ii] = (byte)Character.digit(characters[ii], 16);
        }
        return values;
    }

    /**
     * Set the machines identifier. Currently done by writing a secret file.
     */
    public static void setMachineIdentifier (String ident)
    {
        writeSecretFile(ident);
    }

    /**
     * Read the secret file and get the machine identifier from it. Return null
     * if we have no secret file or can't read from it.
     */
    protected static String readSecretFile ()
    {
        File file = findSecretFile();
        String ident = null;

        try {
            BufferedReader fin = new BufferedReader(new FileReader(file));
            ident = fin.readLine();
            fin.close();
        } catch (Exception e) {
            // if we can't read the file, just return null
            return null;
        }

        // recompute old-style idents
        if (ident != null && ident.substring(1).equals(
                StringUtil.sanitize(ident.substring(1), "[a-f0-9]"))) {
            file.delete();
            return null;
        }

        return ident;
    }

    /**
     * Write the specified ident to the secret file.
     */
    protected static void writeSecretFile (String ident)
    {
        File file = findSecretFile();

        try {
            // we can't overwrite existing ident files once they're read-only
            file.delete();
            file.createNewFile();

            BufferedWriter fout = new BufferedWriter(new FileWriter(file));
            fout.write(ident);
            fout.close();
            file.setLastModified(System.currentTimeMillis() - TWO_MONTHS);
            file.setReadOnly();

        } catch (Exception e) {
            // if we can't write the file, there's no utility in complaining
        }
    }

    /**
     * Sort of sneaky helper for read/write(ing) of secret files.  It iterates
     * over the possible locations for the file and tries to create a file in
     * each one.  We then check if there is really a file there.  If there is,
     * we return a File object to it.  So for reading, no files will be created
     * since the file will already exist.  For writing it will either create
     * the new file, or return the previous one.  It's magic.
     */
    protected static File findSecretFile ()
    {
        String[] locs = (RunAnywhere.isWindows()) ? WINDOWS_LOCS : UNIX_LOCS;
        String homedir = System.getProperty("user.home");

        for (String path : locs) {
            File loc = new File(path.replace("~", homedir));
            try {
                loc.createNewFile();
            } catch (IOException e) {
                // we don't really care how it failed, but we know the location
                // is not suitable for the secret file
                continue;
            }
            if (loc.exists()) {
                return loc;
            }
        }
        return null;
    }

    /**
     * Check and see if the supplied ident is in the bogus ident list.
     */
    public static boolean isBogusIdent (String ident)
    {
        return bogusIdents.contains(ident);
    }

    /**
     * A list of bogus idents.  A bogus ident is one that is made from a single
     * MAC address that is known to be wack as defined in the {@link MACUtil}
     * class.  Or an ident for which there is a large group of users which make
     * it not unique.  These idents will be ignored, old secret files with
     * them will be deleted and new unique idents will be computed or acquired
     * from the server.
     */
    protected static Set<String> bogusIdents = Sets.newHashSet(
        NO_IDENT,
        "9YKC7D3I6WM6KA6WUKTrmGf69NbT0jHA",
        "YpUsOXlKg+UgjE/7IGmYRdrTAsHJjiOZ",
        "MQPUvldivYA5NWPfjD06BvOB04BbNSpW",
        "H6QKfzaYjIzC3Hvfs3ZyOasRW5rYYjoi",
        "pepl+wwT8MBilD90Vaky+E0fosM1Ic9A",
        "STsP+ZW4ZzRb76rQdsLn34gbfqiO4f+F",
        "BgYVLX2n65LbvJ3qww03sGHM6jx4X42l",
        "KAHmuTm/KK5JBuU248oU21SltK+0t5Hn",
        "l2oqxOv/sISNYZSA5zZWrGCXl5boZ3cA",
        "Dx/CPCIDjYt4NNvoX/UV+TMtXIfiMmV2",
        "3fQFby4Bvj++pUsIsYM4CwDBvGDFtY4P",
        "vewfyrMt/ojZp/v5+0ovES/5AvDHz702",
        "13fN/MCeZLYRJDOQzpOVjTmMqDRcr4IQ",
        "FOnvFUunXVaWpYqo8V+dEcXPQA9wMj6t",
        "pGHG2zQaCXiuv9BZ4QiD+Oz7lY31GMpi",
        "XONdkrW8/Ye3zkJZZHd/SmX1pzsMSeBP",
        "8uxLC8kg0IxhqKPy5w8uISszAoLxq5AF",
        "YqXRxbA0aAcY+2Xvy8eu8hlvllJWdiUl",
        "JAkwPG/evcw93tqNPXyshZbTuLOJPoM9",
        "chFjoYxmA7NKI4S+sg/ySDxkAPFhycwR",
        "nDtzsweludiYtMKL2Itb0vwtWkYCyJFt",
        "di3HZs2/5zA522BsxeUo45RzfWt16185",
        "baBIxytENJa29TbEYxn2HNrP3HjKJAKG",
        "me8nehdCWApjqkIZeReBMgFSbPM4cBYp",
        "EeczH9D1j3H9q4Lvk0ttWpbLpkCskKxz",
        "J4acsUt4scEL4TrZPK3imE30qaD/qnJP",
        "psn75+6k6ZRjXnrGv9BpE3jyDYen5Iax"
    );

    /** Two months in milliseconds. */
    protected static long TWO_MONTHS = 1000L * 60 * 60 * 24 * 60;

    /** Locations for which we consider placing our ident file on Windows. */
    protected static String[] WINDOWS_LOCS = {
        "C:\\WINDOWS\\Application Data\\Identities\\hash.dat",
        "C:\\WINDOWS\\Application Data\\hash.dat",
        "C:\\Documents and Settings\\All Users\\Application\\ Data\\hash.dat",
        "C:\\Documents and Settings\\All Users\\hash.dat",
        "C:\\WINNT\\java\\hash.dat",
        "C:\\WINNT\\hash.dat",
        "C:\\WINDOWS\\hash.dat",
        "~\\hash.dat",
    };

    /** Locations for which we consider placing our ident file on Unix (or
     * MacOS or any non-Windows platform really). */
    protected static String[] UNIX_LOCS = {
        "~/.mozilla/fonts/.xfs_ath_42",
        "~/.netscape/archive/.index_42",
        "~/.galeon/sessions/.sessionAuth_42",
        "~/.gnome/.sessionAuth_42",
        "~/.ypp_42",
        "/tmp/.sess_8475c70a4b3161138aaa4034bb59a005",
    };

    // TODO we probably want to obfuscate the locations in some fashion so
    // people can't do strings and find where the secret files are stashed

    /** The value used for the Z/64Z quick substitution scheme - must be an odd
     * number between 0 and 64. */
    protected static final int ENCRYPTION_MULTIPLIER = 29;

    /** An appropriate value generated from the encryption multiplier such that
     * (ENCRYPTION_MULTIPLIER * DECRYPTION_MULTIPLIER) % 64 = 1.  This can be
     * done using the Extended Euclidean Algorithm. */
    protected static final int DECRYPTION_MULTIPLIER = 53;

    /** Allows conversion of ASCII characters to appropriate base 64 values
     * From private field of org.apache.commons.codec.binary.Base64 */
    protected static byte[] CHAR_TO_BASE_64 = new byte[255];

    /** Allows conversion of base 64 values to ASCII characters.
     * From private field of org.apache.commons.codec.binary.Base64 */
    protected static byte[] BASE_64_TO_CHAR = new byte[64];

    // Populates the lookup and character arrays
    // from org.apache.commons.codec.binary.Base64
    static {
        for (int i = 0; i < 255; i++) {
            CHAR_TO_BASE_64[i] = (byte) -1;
        }
        for (int i = 'Z'; i >= 'A'; i--) {
            CHAR_TO_BASE_64[i] = (byte) (i - 'A');
        }
        for (int i = 'z'; i >= 'a'; i--) {
            CHAR_TO_BASE_64[i] = (byte) (i - 'a' + 26);
        }
        for (int i = '9'; i >= '0'; i--) {
            CHAR_TO_BASE_64[i] = (byte) (i - '0' + 52);
        }
        CHAR_TO_BASE_64['+'] = 62;
        CHAR_TO_BASE_64['/'] = 63;

        for (int i = 0; i <= 25; i++) {
            BASE_64_TO_CHAR[i] = (byte) ('A' + i);
        }
        for (int i = 26, j = 0; i <= 51; i++, j++) {
            BASE_64_TO_CHAR[i] = (byte) ('a' + j);
        }
        for (int i = 52, j = 0; i <= 61; i++, j++) {
            BASE_64_TO_CHAR[i] = (byte) ('0' + j);
        }
        BASE_64_TO_CHAR[62] = (byte) '+';
        BASE_64_TO_CHAR[63] = (byte) '/';
    }
}
