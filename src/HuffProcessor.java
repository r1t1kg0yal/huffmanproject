// Ritik Goyal
// Bryan Tong

import java.util.*;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
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
	public void decompress(BitInputStream in, BitOutputStream out) {
		//check for HUFF_NUMBER
		if(in.readBits(BITS_PER_INT) != HUFF_NUMBER)
			throw new HuffException("HUFF_NUMBER is not presented!");
		//recreate the Hufftree from header
		HuffNode root = readHeader(in);
		// parse body of compressed file
		HuffNode current = root;
		while(true){
			int bit = in.readBits(1);
			if(bit == -1)
				break;
			if(bit == 1)
				current = current.myRight;
			else 
				current = current.myLeft;
			if(current.myLeft == null && current.myRight == null){
				if(current.myValue == PSEUDO_EOF)
					return;
				else { 
					out.writeBits(BITS_PER_WORD, current.myValue);
				    current = root;
				    }
			}
		}
		
		out.close();
	}
	
	private HuffNode readHeader(BitInputStream in){
		if(in.readBits(1) == 0){
			HuffNode left = readHeader(in);
			HuffNode right = readHeader(in);
			return new HuffNode(-1, 0, left, right);
		} else {
			return new HuffNode(in.readBits(BITS_PER_WORD+1), 0);
		}
	}
	
}