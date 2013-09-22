package rx.libgdx.events;

public abstract class TouchEvent implements InputEvent {
    private final int screenX;
    private final int screenY;
    private final int pointer;

    public TouchEvent(int screenX, int screenY, int pointer) {
        this.screenX = screenX;
        this.screenY = screenY;
        this.pointer = pointer;
    }
    
    public int getScreenX() {
        return screenX;
    }
    
    public int getScreenY() {
        return screenY;
    }
    
    public int getPointer() {
        return pointer;
    }
}
