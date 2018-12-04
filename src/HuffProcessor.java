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
	public void compress(BitInputStream in, BitOutputStream out) {
		
		int [] counts = readforCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeTreeHeader(root, out);
		
		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();
	}
	
	/**
	 * Determines frequencies 
	 * @param in 
	 * @return int array of frequencies
	 */
	private int[] readforCounts(BitInputStream in) {
		
		int [] freqs = new int[ALPH_SIZE +1];
		freqs[PSEUDO_EOF] = 1;
		
		while(true) {
		   int value = in.readBits(BITS_PER_WORD);
		   if (value == -1) break;
		   else {
			   freqs[value] += 1;
		   }
		}
		
		return freqs;
			
	}
	
	/**
	 * PriorityQueue enables greedy algorithm
	 * @param freqs
	 * @return HuffMan Tree/Trie
	 */
	private HuffNode makeTreeFromCounts(int[] freqs) {
		
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		
		for(int k = 0; k < freqs.length; k++) {
			if (freqs[k] > 0) //only add nodes with non-zero weights to pq
				pq.add(new HuffNode(k, freqs[k], null, null));
			
		}
		
		while (pq.size() > 1) {
			
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			HuffNode t = new HuffNode(-1, left.myWeight + right.myWeight, left, right); 
			pq.add(t);
			
		}
		
		HuffNode root = pq.remove();
		return root;
	}
	
	/**
	 * Return String array of bit-stream codes
	 * @param root is HuffNode tree object
	 * @return
	 */
	private String [] makeCodingsFromTree(HuffNode root) {
		
		String[] codes = new String[ALPH_SIZE +1];
		codingHelper(root, "", codes);
		
        return codes;
	}
	
	/**
	 * Reads tree, creates encoding pathway determined by 0s (left) and 1s (right)
	 * @param root is the HuffNode tree object
	 * @param path is the path created from the tree
	 * @param codes is a string array of the codes
	 */
	private void codingHelper(HuffNode root, String path, String[] codes) {

		if(root.myLeft == null && root.myRight == null) {
			codes[root.myValue] = path;
			return;
		}
		
        codingHelper(root.myLeft, path + "0", codes); // "0" added to path when left
    	codingHelper(root.myRight, path + "1", codes); // "1" added to path when right
		
	}
	
    /**
     * If node is internal node, writes single bit of zero
     * If node is leaf, writes a single bit of 1 followed by 9 bits of value stored in the leaf
     * @param root is the HuffNode tree object
     * @param out is the codes written 
     */
	private void writeTreeHeader(HuffNode root, BitOutputStream out) {
		
		if(root.myLeft == null && root.myRight == null) {
			
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1 , root.myValue);
			
		}
		
		else {
			
			out.writeBits(1, 0);
			writeTreeHeader(root.myLeft, out);
			writeTreeHeader(root.myRight, out);
			
		}
	}
	
	/**
	 * Reads input and uses codings to encode bit-sequence for characters in input
	 * @param codings, String array containing codes for each character
	 * @param in
	 * @param out
	 */
	private void writeCompressedBits(String [] codings, BitInputStream in, BitOutputStream out) {
		
		in.reset();
		int value = in.readBits(BITS_PER_WORD);
		
		while(value != -1) {
			
			String code = codings[value];
			out.writeBits(code.length(), Integer.parseInt(code, 2));	
			value = in.readBits(BITS_PER_WORD);
			
		}
		
		String last = codings[PSEUDO_EOF];
		out.writeBits(last.length(), Integer.parseInt(last, 2));
				
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