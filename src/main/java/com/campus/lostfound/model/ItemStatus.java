package com.campus.lostfound.model;

public enum ItemStatus {
    OPEN,      // reported, nobody has matched it to anything yet
    MATCHED,   // staff paired this up and gave pickup instructions
    COLLECTED, // the student confirmed they actually got the item — fully done
    CANCELLED  // the student said "I didn't get my item" — report closed, student must reapply
}
