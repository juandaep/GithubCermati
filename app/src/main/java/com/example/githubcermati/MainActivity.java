package com.example.githubcermati;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.githubcermati.api.ApiUrl;
import com.example.githubcermati.api.Service;
import com.example.githubcermati.model.Item;
import com.example.githubcermati.model.ItemResponse;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    public String keyword ="", prevKeyword = "";
    public int pageNumber, prevResponseLength;

    private MaterialSearchView mMaterialSearchView;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeContainer;

    TextView Disconnected;
    LinearLayoutManager linearLayoutManager;
    PublishSubject<String> querySearchSubject;

    List<Item> itemList = new ArrayList<Item>();
    RecyclerView.Adapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        linearLayoutManager= new LinearLayoutManager(getApplicationContext());

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.smoothScrollToPosition(0);

        initViews();
        initRecyclerView();
    }

    private void initToolbar(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initMaterialSearchView(){
        mMaterialSearchView = findViewById(R.id.searchView);
        mMaterialSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(!newText.isEmpty()) {
                    querySearchSubject.onNext(newText);
                }else{
                    itemList.clear();
                    prevKeyword = keyword;
                }
                return false;
            }
        });
    }

    private void initQuerySearchSubject(){
        querySearchSubject = PublishSubject.create();
        querySearchSubject.debounce(1, TimeUnit.SECONDS).subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onNext(String s) {
                //will clear the list, making user arrive at the start of the page due to changing keyword
                itemList.clear();
                //the new keyword
                keyword = s;
                pageNumber = 1;
                getData();
            }

            @Override
            public void onError(Throwable e) {}

            @Override
            public void onComplete() {}
        });
    }

    private void initViews(){

        initToolbar();
        initMaterialSearchView();
        initQuerySearchSubject();
    }

    private void initRecyclerView() {
        adapter = new AdapterItem(itemList, getApplicationContext());
        recyclerView.setAdapter(adapter);DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                linearLayoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        EndlessScroll scrollListener = new EndlessScroll(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                loadNextData(page);
            }
        };
        recyclerView.addOnScrollListener(scrollListener);
    }

    private void getData(){
        Disconnected = findViewById(R.id.disconnected);
        try {
            Service apiService = ApiUrl.getClient().create(Service.class);

            //getting the itemResponse using getUserList method from the service
            Call<ItemResponse> call = apiService.getUserList(keyword, pageNumber);
            call.enqueue(new Callback<ItemResponse>() {

                //Got Response
                @Override
                public void onResponse(Call<ItemResponse> call, Response<ItemResponse> response) {

                    //Response success code == 200
                    if (response.code() == 200) {

                        //check whether there is response but return empty user
                        assert response.body() != null;
                        if(!prevKeyword.equals(keyword)  && prevResponseLength == response.body().getItems().size()){
                            itemList.clear();
                            adapter.notifyDataSetChanged();
                            Toast.makeText(MainActivity.this, "Please use another keyword to search for user", Toast.LENGTH_SHORT ).show();
                        }

                        //there is response and there is / are several user(s)
                        else{
                            List<Item> items = response.body().getItems();

                            //add the list item ,, if we change pagenumber from loadNextDataFromApi(),, it will only add the list
                            itemList.addAll(items);
                            adapter.notifyDataSetChanged();
                            prevKeyword = keyword;
                        }
                    }

                    //Response failed because of rate limit,, response code == 403
                    else if(response.code() == 403){
                        Toast.makeText(MainActivity.this, "Rate limit reached, please wait another minute to make another search", Toast.LENGTH_SHORT).show();
                        adapter.notifyDataSetChanged();
                    }
                }

                //Got no response,, usually because there's no expected response from api or the network connection
                @Override
                public void onFailure(Call<ItemResponse> call, Throwable throwable) {
                    Log.d("Error", Objects.requireNonNull(throwable.getMessage()));
                    Toast.makeText(MainActivity.this, throwable.toString(), Toast.LENGTH_SHORT).show();
                    Disconnected.setVisibility(View.VISIBLE);
                }
            });
        }catch (Exception e){
            Log.d("Error", Objects.requireNonNull(e.getMessage()));
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    //To load the next page of the api
    private void loadNextData(int page) {
        pageNumber++;
        getData();
    }

    //creating the search menu view
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.searchMenu);
        mMaterialSearchView.setMenuItem(menuItem);
        return super.onCreateOptionsMenu(menu);
    }
}

