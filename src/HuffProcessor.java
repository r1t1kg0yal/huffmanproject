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
		//count characters in file
		int[] count = new int[ALPH_SIZE];
		while(true){
			int character = in.readBits(BITS_PER_WORD);
			if(character == -1)
				break;
			count[character]++;
		}
		in.reset();
		//create Huffman tree
		PriorityQueue<HuffNode> HuffmanTree = new PriorityQueue<HuffNode>();
		for(int i = 0; i < ALPH_SIZE; i++){
			if(count[i] != 0){
				HuffmanTree.add(new HuffNode(i, count[i]));
			}
		}
		HuffmanTree.add(new HuffNode(PSEUDO_EOF, 0));
		while(HuffmanTree.size() > 1){
			HuffNode sub1 = HuffmanTree.poll();
			HuffNode sub2 = HuffmanTree.poll();
			HuffmanTree.add(new HuffNode(-1, sub1.myWeight+sub2.myWeight, sub1, sub2));
		}
		//traverse tree and extract codes
		String[] codes = new String[ALPH_SIZE+1];
		HuffNode root = HuffmanTree.poll();
		extractCodes(root, "", codes);
		//write the header
		out.writeBits(BITS_PER_INT, HUFF_NUMBER);
		writeHeader(root, out);
		//compress
		while(true){
			int character = in.readBits(BITS_PER_WORD);
			if(character == -1)
				break;
			String code = codes[character];
			out.writeBits(code.length(), Integer.parseInt(code, 2));
		}
		//write the pseudo-EOF
		String code = codes[PSEUDO_EOF];
		out.writeBits(code.length(), Integer.parseInt(code, 2));
	}
	
	private void extractCodes(HuffNode current, String path, String[] codes) {
		if(current.myLeft == null && current.myRight == null){
			codes[current.myValue] = path;
			return;
		}
		extractCodes(current.myLeft, path + 0, codes);
		extractCodes(current.myRight, path + 1, codes);
	}
	
	private void writeHeader(HuffNode current, BitOutputStream out){
		if(current.myLeft == null && current.myRight == null){
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, current.myValue);
			return;
		}
		out.writeBits(1, 0);
		writeHeader(current.myLeft, out);
		writeHeader(current.myRight, out);
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
	
	public HuffNode readTreeHeader(BitInputStream in) {
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
	
	public HuffNode readCompressedBits(HuffNode root, BitInputStream in, BitOutputStream out) {
		
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