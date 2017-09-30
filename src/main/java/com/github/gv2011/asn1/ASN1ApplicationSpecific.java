package com.github.gv2011.asn1;

import static com.github.gv2011.util.bytes.ByteUtils.newBytes;


import com.github.gv2011.util.bytes.Bytes;

/**
 * Base class for an application specific object
 */
public abstract class ASN1ApplicationSpecific
    extends ASN1Primitive
{
    protected final boolean   isConstructed;
    protected final int       tag;
    protected final Bytes     octets;

    ASN1ApplicationSpecific(
        final boolean isConstructed,
        final int tag,
        final Bytes octets)
    {
        this.isConstructed = isConstructed;
        this.tag = tag;
        this.octets = octets;
    }

    /**
     * Return an ASN1ApplicationSpecific from the passed in object, which may be a byte array, or null.
     *
     * @param obj the object to be converted.
     * @return obj's representation as an ASN1ApplicationSpecific object.
     */
    public static ASN1ApplicationSpecific getInstance(final Object obj)
    {
        if (obj == null || obj instanceof ASN1ApplicationSpecific)
        {
            return (ASN1ApplicationSpecific)obj;
        }
        else if (obj instanceof Bytes)
        {
            return ASN1ApplicationSpecific.getInstance(ASN1Primitive.fromByteArray((Bytes)obj));
        }

        throw new IllegalArgumentException("unknown object in getInstance: " + obj.getClass().getName());
    }

    protected static int getLengthOfHeader(final Bytes data)
    {
        final int length = data.getByte(1) & 0xff; // TODO: assumes 1 byte tag

        if (length == 0x80)
        {
            return 2;      // indefinite-length encoding
        }

        if (length > 127)
        {
            final int size = length & 0x7f;

            // Note: The invalid long form "0xff" (see X.690 8.1.3.5c) will be caught here
            if (size > 4)
            {
                throw new IllegalStateException("DER length more than 4 bytes: " + size);
            }

            return size + 2;
        }

        return 2;
    }

    /**
     * Return true if the object is marked as constructed, false otherwise.
     *
     * @return true if constructed, otherwise false.
     */
    @Override
    public boolean isConstructed()
    {
        return isConstructed;
    }

    /**
     * Return the contents of this object as a byte[]
     *
     * @return the encoded contents of the object.
     */
    public Bytes getContents()
    {
        return octets;
    }

    /**
     * Return the tag number associated with this object,
     *
     * @return the application tag number.
     */
    public int getApplicationTag()
    {
        return tag;
    }

    /**
     * Return the enclosed object assuming explicit tagging.
     *
     * @return  the resulting object
     * @throws IOException if reconstruction fails.
     */
    @SuppressWarnings("resource")
    public ASN1Primitive getObject()
    {
        return new ASN1InputStream(getContents()).readObject();
    }

    /**
     * Return the enclosed object assuming implicit tagging.
     *
     * @param derTagNo the type tag that should be applied to the object's contents.
     * @return  the resulting object
     * @throws IOException if reconstruction fails.
     */
    @SuppressWarnings("resource")
    public ASN1Primitive getObject(final int derTagNo)
    {
        if (derTagNo >= 0x1f)
        {
            throw new ASN1Exception("unsupported tag number");
        }

        final Bytes orig = this.getEncoded();
        final byte[] tmp = replaceTagNumber(derTagNo, orig);

        if ((orig.getByte(0) & BERTags.CONSTRUCTED) != 0)
        {
            tmp[0] |= BERTags.CONSTRUCTED;
        }

        return new ASN1InputStream(newBytes(tmp)).readObject();
    }

    @Override
    int encodedLength()
    {
        return StreamUtil.calculateTagLength(tag) + StreamUtil.calculateBodyLength(octets.size()) + octets.size();
    }

    /* (non-Javadoc)
     * @see org.bouncycastle.asn1.ASN1Primitive#encode(org.bouncycastle.asn1.DEROutputStream)
     */
    @Override
    void encode(final ASN1OutputStream out)
    {
        int classBits = BERTags.APPLICATION;
        if (isConstructed)
        {
            classBits |= BERTags.CONSTRUCTED;
        }

        out.writeEncoded(classBits, tag, octets);
    }

    @Override
    boolean asn1Equals(
        final ASN1Primitive o)
    {
        if (!(o instanceof ASN1ApplicationSpecific))
        {
            return false;
        }

        final ASN1ApplicationSpecific other = (ASN1ApplicationSpecific)o;

        return isConstructed == other.isConstructed
            && tag == other.tag
            && octets.equals(other.octets);
    }

    @Override
    public int hashCode()
    {
        return (isConstructed ? 1 : 0) ^ tag ^ octets.hashCode();
    }

    private byte[] replaceTagNumber(final int newTag, final Bytes input)
    {
        int tagNo = input.getByte(0) & 0x1f;
        int index = 1;
        //
        // with tagged object tag number is bottom 5 bits, or stored at the start of the content
        //
        if (tagNo == 0x1f)
        {
            tagNo = 0;

            int b = input.getByte(index++) & 0xff;

            // X.690-0207 8.1.2.4.2
            // "c) bits 7 to 1 of the first subsequent octet shall not all be zero."
            if ((b & 0x7f) == 0) // Note: -1 will pass
            {
                throw new ASN1ParsingException("corrupted stream - invalid high tag number found");
            }

            while ((b >= 0) && ((b & 0x80) != 0))
            {
                tagNo |= (b & 0x7f);
                tagNo <<= 7;
                b = input.getByte(index++) & 0xff;
            }

//            tagNo |= (b & 0x7f);
        }

        final byte[] tmp = new byte[input.size() - index + 1];

        System.arraycopy(input, index, tmp, 1, tmp.length - 1);

        tmp[0] = (byte)newTag;

        return tmp;
    }
}