class Bar {
    void main(boolean arg) {
        boolean myBoolean;

        // ruleid:hardcoded-conditional
        if (myBoolean = true) {
            continue;
        }
        // note that with new constant propagation, myBoolean is assumed
        // to true below
    }
}