package uk.ac.plymouth.interiordesign.Fragments

// Generic interface for returning data across fragments
interface DataReturnInterface<T> {
    fun returnData(data : T)
}