package Project.Client.Interfaces;

import java.util.List;// vvh-12/09/24 Import statement for the List interface

public interface ICategoryEvents extends IGameEvents{
    void onReceiveCategories(List<String> categories); //vvh-12/09/24 Called when the client receives a list of categories

    void onCategorySelected(String category);//vvh-12/09/24 Called when a specific category is selected by the client
}
