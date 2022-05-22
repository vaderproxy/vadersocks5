package struct;

import java.util.ArrayList;

public class PHPScript {

    public String name;
    public String alias;
    public ArrayList<String> text = new ArrayList();
    public ArrayList<String> text2 = new ArrayList();

    @Override
    public String toString() {
        if (name != null) {
            return name;
        }
        if (alias != null) {
            return alias;
        }
        return "empty";
    }

}
