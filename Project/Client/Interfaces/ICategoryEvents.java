package Project.Client.Interfaces;

import java.util.List;

public interface ICategoryEvents extends IGameEvents{
    void onReceiveCategories(List<String> categories);

    void onCategorySelected(String category);
}
