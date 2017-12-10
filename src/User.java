/**
 * [User.java]
 * holds all the data for a single user
 * @author Sasha Maximovitch
 * @date November 9th, 2017
 */

public class User {

    // global variables
    private String name;// user's name
    private int listNum;// user's list number

    /**
     * getListNum
     * @return the list number of the user
     */
    public int getListNum() {
        return listNum;
    }

    /**
     * setListNum
     * @param listNum the list number to be set for the user
     */
    public void setListNum(int listNum) {
        this.listNum = listNum;
    }

    /**
     * getName
     * @return returns the name of the user
     */
    public String getName() {
        return name;
    }

    /**
     * setName
     * @param name to be set for the user
     */
    public void setName(String name) {
        this.name = name;
    }

}
