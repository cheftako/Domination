package com.linkedin.domination.api;

/**
 * Relative size of planets or fleets. Small indicates a value between 0-19. Medium is a value between 20-49, Large
 * is a value 50 or greater.
 */
public enum Size {
    SMALL,
    MEDIUM,
    LARGE;

    public static Size getSizeForNumber(int unit)
    {
        if (unit < 20){
            return SMALL;
        } else if (unit < 50) {
            return MEDIUM;
        } else{
            return LARGE;
        }
    }
}
