package com.github.gv2011.asn1.util.encoders;


import static com.github.gv2011.util.ex.Exceptions.run;

import java.io.OutputStream;

import com.github.gv2011.util.bytes.Bytes;

/**
 * A streaming Hex encoder.
 */
public class HexEncoder implements Encoder{

    protected final byte[] encodingTable =
    {
        (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6', (byte)'7',
        (byte)'8', (byte)'9', (byte)'a', (byte)'b', (byte)'c', (byte)'d', (byte)'e', (byte)'f'
    };

    /*
     * set up the decoding table.
     */
    protected final byte[] decodingTable = new byte[128];

    protected void initialiseDecodingTable()
    {
        for (int i = 0; i < decodingTable.length; i++)
        {
            decodingTable[i] = (byte)0xff;
        }

        for (int i = 0; i < encodingTable.length; i++)
        {
            decodingTable[encodingTable[i]] = (byte)i;
        }

        decodingTable['A'] = decodingTable['a'];
        decodingTable['B'] = decodingTable['b'];
        decodingTable['C'] = decodingTable['c'];
        decodingTable['D'] = decodingTable['d'];
        decodingTable['E'] = decodingTable['e'];
        decodingTable['F'] = decodingTable['f'];
    }

    public HexEncoder()
    {
        initialiseDecodingTable();
    }

    /**
     * encode the input data producing a Hex output stream.
     *
     * @return the number of bytes produced.
     */
    @Override
    public int encode(
        final Bytes                data,
        final int                    off,
        final int                    length,
        final OutputStream    out)
    {
        for (int i = off; i < (off + length); i++)
        {
            final int    v = data.getByte(i) & 0xff;
            run(()->{
              out.write(encodingTable[(v >>> 4)]);
              out.write(encodingTable[v & 0xf]);
            });
        }

        return length * 2;
    }

    private static boolean ignore(
        final char    c)
    {
        return c == '\n' || c =='\r' || c == '\t' || c == ' ';
    }

    /**
     * decode the Hex encoded byte data writing it to the given output stream,
     * whitespace characters will be ignored.
     *
     * @return the number of bytes produced.
     */
    @Override
    public int decode(
        final Bytes          data,
        final int             off,
        final int             length,
        final OutputStream    out)
    {
        int     outLen = 0;

        int     end = off + length;

        while (end > off)
        {
            if (!ignore((char)data.getByte(end - 1)))
            {
                break;
            }

            end--;
        }

        int i = off;
        while (i < end)
        {
            while (i < end && ignore((char)data.getByte(i)))
            {
                i++;
            }

            final byte    b1, b2;

            b1 = decodingTable[data.getByte(i++)];

            while (i < end && ignore((char)data.getByte(i)))
            {
                i++;
            }

            b2 = decodingTable[data.getByte(i++)];

            if ((b1 | b2) < 0)
            {
                throw new RuntimeException("invalid characters encountered in Hex data");
            }

            run(()->out.write((b1 << 4) | b2));

            outLen++;
        }

        return outLen;
    }

    /**
     * decode the Hex encoded String data writing it to the given output stream,
     * whitespace characters will be ignored.
     *
     * @return the number of bytes produced.
     */
    @Override
    public int decode(
        final String          data,
        final OutputStream    out)
    {
        int     length = 0;

        int     end = data.length();

        while (end > 0)
        {
            if (!ignore(data.charAt(end - 1)))
            {
                break;
            }

            end--;
        }

        int i = 0;
        while (i < end)
        {
            while (i < end && ignore(data.charAt(i)))
            {
                i++;
            }

            final byte    b1, b2;
            b1 = decodingTable[data.charAt(i++)];

            while (i < end && ignore(data.charAt(i)))
            {
                i++;
            }

            b2 = decodingTable[data.charAt(i++)];

            if ((b1 | b2) < 0)
            {
                throw new RuntimeException("invalid characters encountered in Hex string");
            }

            run(()->out.write((b1 << 4) | b2));

            length++;
        }

        return length;
    }
}