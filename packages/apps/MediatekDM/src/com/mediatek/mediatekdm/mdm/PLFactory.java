
package com.mediatek.mediatekdm.mdm;

/**
 * An abstract porting layer factory interface.
 */
public interface PLFactory {

    /**
     * Allocate a new Download Package instance.
     * 
     * @return a new instance of Download Package.
     */
    PLDlPkg getDownloadPkg();

    /**
     * Allocate a new Registry instance.
     * 
     * @return a new instance of Registry.
     */
    PLRegistry getRegistry();

    /**
     * Allocate a new Storage instance.
     * 
     * @return a new instance of Storage.
     */
    PLStorage getStorage();
}
