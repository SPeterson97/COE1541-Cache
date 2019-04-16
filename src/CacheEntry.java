public class CacheEntry {
	
	public String data;
	public int validBit;
	public int dirtyBit;
	public int index;
	public int tag;
	public int LRU;
	public int writes;
	public int number;
	public boolean blocked;
	public int cycles = 10000;
	
	public CacheEntry(String instruction, int number) {
		this.data = instruction;
		this.number = number;
		this.validBit = 0;
		this.dirtyBit = 0;
		this.index = 0;
		this.tag = -1;
		this.LRU = -1;
		this.writes = 0;
		this.blocked = false;
	}

	public boolean getBlocked(){
		return this.blocked;
	}

	public void setBlocked(boolean blocked){
		this.blocked = blocked;
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
		this.validBit = vb;
	}
	
	public int getDirtyBit() {
		return this.dirtyBit;
	}
	
	public void updateDirtyBit(int db) {
		this.dirtyBit = db;
	}
	
	public int getIndex() {
		return this.index;
	}
	
	public void updateIndex(int index) {
		this.index = index;
	}
	
	public int getTag() {
		return this.tag;
	}
	
	public void updateTag(int tag) {
		this.tag = tag;
	}

	public int getLRU(){
		return this.LRU;
	}

	public void updateLRU(int LRU){ this.LRU = LRU; }
}
