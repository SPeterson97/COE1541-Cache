public class CacheEntry {
	
	public String data;
	public int validBit;
	public int dirtyBit;
	public int index;
	public int tag;
	public int LRU;
	public int writes;
	
	public CacheEntry(String instruction) {
		this.data = instruction;
		this.validBit = 0;
		this.dirtyBit = 0;
		this.index = 0;
		this.tag = 0;
		this.LRU = -1;
		this.writes = 0;
	}
	
	public boolean tagEquals(int tag) {
		if(this.tag != tag) {
			return false;
		}
		return true;
	}

	public void write(){ this.writes++; }

	public String getInstruction(){ return this.data; }
	
	public String getData() {
		return this.data;
	}
	
	public void updateData(String newData) {
		this.data = newData;
	}
	
	public int getValidBit() {
		return this.validBit;
	}
	
	public void updateValidBit(int vb) {
		this.validBit = validBit;
	}
	
	public int getDirtyBit() {
		return this.dirtyBit;
	}
	
	public void updateDirtyBit(int db) {
		this.dirtyBit = dirtyBit;
	}
	
	public int getIndex() {
		return this.index;
	}
	
	public void updateIndex(int index) {
		this.index = index;
	}
	
	public int getTag() {
		return this.index;
	}
	
	public void updateTag(int tag) {
		this.tag = index;
	}

	public int getLRU(){
		return this.LRU;
	}

	public void updateLRU(int LRU){ this.LRU = LRU; }
}
