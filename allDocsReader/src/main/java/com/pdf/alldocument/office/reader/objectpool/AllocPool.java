package com.pdf.alldocument.office.reader.objectpool;

import java.util.Vector;

public class AllocPool {

    public AllocPool(IMemObj prototype, int capacity, int capacityIncrement) {
        this.prototype = prototype;
        free = new Vector<>(capacity, capacityIncrement);
    }


    public synchronized IMemObj allocObject() {
        if (free.size() > 0) {
            IMemObj obj = free.remove(0);
            return obj;
        }
        IMemObj obj = prototype.getCopy();
        return obj;
    }


    public synchronized void free(IMemObj obj) {
        free.add(obj);
    }


    public synchronized void dispose() {
        if (free != null) {
            free.clear();
        }
    }

    private IMemObj prototype;

    private Vector<IMemObj> free;
}
