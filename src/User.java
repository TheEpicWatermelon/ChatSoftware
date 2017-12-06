import java.util.List;

/**
 * Created by sasha on 12/2/2017.
 */
public class User {

    private String name;
    private int listNum;

    public int getListNum() {
        return listNum;
    }

    public void setListNum(int listNum) {
        this.listNum = listNum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static void updateNums(List<User> users){
        for (int i = 1; i <= users.size(); i++) {
            users.get(i).setListNum(i);
        }
    }
}
