// Ritik Goyal
// Bryan Tong

import java.util.*;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bit read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){
		int[] counts = readForCount(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root, out);
		
		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();
	}
	private int[] readForCount(BitInputStream x) {
		int[] rayray = new int[ALPH_SIZE + 1];
		while (true) {
			int val = x.readBits(BITS_PER_WORD);
			if (val == -1) break;
			rayray[val] += 1;
		}
		rayray[PSEUDO_EOF] = 1;
		return rayray;
	}
	private HuffNode makeTreeFromCounts(int[] array) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		for (int i = 0; i < array.length; i++) {
			if (array[i] != 0) {
				pq.add(new HuffNode(i, array[i], null, null));
			}
		}
		while (pq.size() > 1) {
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			HuffNode t = new HuffNode(-1, left.myWeight+right.myWeight, left, right);
			pq.add(t);
		}
		HuffNode root = pq.remove();
		return root;
	}
	private String[]makeCodingsFromTree(HuffNode riit){
		String[] encoding = new String[ALPH_SIZE + 1];
		codingHelper(riit, "", encoding);
		return encoding;
	}
	private void codingHelper(HuffNode rit, String s, String[] ray) {
		if (rit == null) {
			throw new HuffException("root is null");
		}
		if (rit.myLeft == null && rit.myRight == null) {
			ray[rit.myValue] = s;
			return;
		}
		codingHelper(rit.myLeft,s+"0", ray);
		codingHelper(rit.myRight,s+"1", ray);
	}
	private void writeHeader(HuffNode reet, BitOutputStream y) {
		if (reet.myLeft != null && reet.myRight != null) {
			y.writeBits(1, 0);
			writeHeader(reet.myLeft, y);
			writeHeader(reet.myRight, y);
		}
		else {
			y.writeBits(1, 1);
			y.writeBits(BITS_PER_WORD + 1, reet.myValue);
		}
	}
	private void writeCompressedBits(String[] arriy, BitInputStream z, BitOutputStream j) {
		while (true) {
			int val = z.readBits(BITS_PER_WORD);
			if (z.readBits(BITS_PER_WORD) == -1) break;
			String code = arriy[val];
			j.writeBits(code.length(), Integer.parseInt(code, 2));
		}
		String cod = arriy[PSEUDO_EOF];
		j.writeBits(cod.length(), Integer.parseInt(cod, 2));
		
	}
	
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){
		
		int bit = in.readBits(BITS_PER_INT);
		
		if(bit != HUFF_TREE) {
			throw new HuffException("illegal header starts with" + bit);
		}
		
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root, in, out);
		out.close();
	}
	
	private HuffNode readTreeHeader(BitInputStream in) {
		int bit = in.readBits(1);
        if (bit == -1) {
            throw new HuffException("bad input, no PSEUDO_EOF");
        }
        if (bit == 0) {
     	    HuffNode left = readTreeHeader(in);
     	    HuffNode right = readTreeHeader(in);
     	    return new HuffNode(0,1,left,right);
     	}
     	else {
     	    int value = in.readBits(BITS_PER_WORD+1);
     	    return new HuffNode(value,0,null,null);
     	}    
	}
	
	private HuffNode readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		
		HuffNode current = root; 
		   while (true) {
		       int bit = in.readBits(1);
		       if (bit == -1) {
		           throw new HuffException("bad input, no PSEUDO_EOF");
		       }
		       else { 
		           if (bit == 0) current = current.myLeft;
		           else current = current.myRight;

		           if (current.myLeft == null && current.myRight == null) {
		               if (current.myValue == PSEUDO_EOF) 
		                   break;   // out of loop
		               else {
		            	   out.writeBits(BITS_PER_WORD, current.myValue);
		                   current = root; // start back after leaf
		               }
		           }
		       }
		   }
		   return root;
	}
}