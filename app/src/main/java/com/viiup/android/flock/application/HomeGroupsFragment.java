package com.viiup.android.flock.application;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.viiup.android.flock.models.UserGroupModel;
import com.viiup.android.flock.models.UserModel;
import com.viiup.android.flock.services.IAsyncGroupResponse;
import com.viiup.android.flock.services.UserService;

import java.util.List;

/**
 * Created by AbdullahMoyeen on 4/11/16.
 */
public class HomeGroupsFragment extends ListFragment implements IAsyncGroupResponse, SearchView.OnQueryTextListener, AdapterView.OnItemClickListener {

    private ListView groupsListView;
    private HomeGroupsCellAdapter adapter;
    private List<UserGroupModel> userGroups;
    private List<UserGroupModel> userGroupsFull;
    private ProgressDialog progressDialog;
    private SwipeRefreshLayout swipeRefreshLayout;
    private UserModel authenticatedUser;

    public HomeGroupsFragment() {
    }

    public static HomeGroupsFragment newInstance() {
        return new HomeGroupsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        progressDialog = ProgressDialog.show(getActivity(), getString(R.string.title_events_groups), getString(R.string.msg_loading_data));
        return inflater.inflate(R.layout.home_groups_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        // Find the swipe refresh layout
        swipeRefreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.swipe_container_groups);

        // Hook up the refresh listener
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayoutListener());

        // Set color skim for refresh ui
        swipeRefreshLayout.setColorSchemeResources(R.color.colorFlockBird1, R.color.colorFlockBird2, R.color.colorFlockBird3, R.color.colorFlockBird4);

        SharedPreferences mPref = getActivity().getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        String authenticatedUserJson = mPref.getString("authenticatedUserJson", null);

        Gson gson = new Gson();
        authenticatedUser = gson.fromJson(authenticatedUserJson, UserModel.class);

        UserService userService = new UserService();
        userService.getUserGroupsByUserId(authenticatedUser.getUserId(), this);

        groupsListView = getListView();
        groupsListView.setOnItemClickListener(this);

        // Attach scroll listener for list view to block swipe refresh from activating on scroll up
        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                boolean enabled = false;
                if (getListView() != null && getListView().getChildCount() > 0) {
                    // check if the first item of the list is visible
                    boolean firstItemVisible = getListView().getFirstVisiblePosition() == 0;
                    // check if the top of the first item is visible
                    boolean topOfFirstItemVisible = getListView().getChildAt(0).getTop() == 0;
                    // enabling or disabling the refresh layout
                    enabled = firstItemVisible && topOfFirstItemVisible;
                }

                // Enable the layout
                swipeRefreshLayout.setEnabled(enabled);
            }
        });
    }

    public boolean onQueryTextChange(String newText) {
        filterGroups(newText);
        return true;
    }

    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    public void filterGroups(String newText) {

        if (userGroupsFull != null && newText != null) {

            userGroups.clear();

            for (UserGroupModel userGroup : userGroupsFull) {
                if (userGroup.group.getGroupName().toLowerCase().contains(newText.toLowerCase())
                        || userGroup.group.getGroupDescription().toLowerCase().contains(newText.toLowerCase())
                        || userGroup.group.getGroupCategory().toLowerCase().contains(newText.toLowerCase())) {
                    userGroups.add(userGroup);
                }
            }

            adapter = new HomeGroupsCellAdapter(getActivity(), getListView(), userGroups, userGroupsFull);
            setListAdapter(adapter);
        }
    }

    public void filterMyGroups() {

        if (userGroupsFull != null) {

            userGroups.clear();

            for (UserGroupModel userGroup : userGroupsFull) {
                if (userGroup.getGroupMembershipStatus().equals("A")) {
                    userGroups.add(userGroup);
                }
            }

            adapter = new HomeGroupsCellAdapter(getActivity(), getListView(), userGroups, userGroupsFull);
            setListAdapter(adapter);
        }
    }

    public void resetToFull() {
        Gson gson = new Gson();
        String userGroupsJson = gson.toJson(userGroupsFull);
        userGroups = gson.fromJson(userGroupsJson, new TypeToken<List<UserGroupModel>>() {
        }.getType());
        adapter = new HomeGroupsCellAdapter(getActivity(), getListView(), userGroups, userGroupsFull);
        setListAdapter(adapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Gson gson = new Gson();
        UserGroupModel userGroup = userGroups.get(position);
        String userGroupJson = gson.toJson(userGroup);

        Intent groupDetailsIntent = new Intent(this.getContext(), GroupDetailsActivity.class);
        groupDetailsIntent.putExtra("userGroupJson", userGroupJson);
        startActivityForResult(groupDetailsIntent, position);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {

            String membershipStatus = data.getStringExtra("membershipStatus");

            if (membershipStatus != null) {

                UserGroupModel changedUserGroup = userGroups.get(requestCode);
                int pendingMemberCount = changedUserGroup.group.getPendingMemberCount();
                int activeMemberCount = changedUserGroup.group.getActiveMemberCount();

                if (membershipStatus.equals("P")) {

                    changedUserGroup.setGroupMembershipStatus("P");
                    changedUserGroup.group.setPendingMemberCount(pendingMemberCount + 1);
                    for (UserGroupModel userGroupFromFull : userGroupsFull) {
                        if (userGroupFromFull.group.getGroupId() == changedUserGroup.group.getGroupId()) {
                            userGroupFromFull.setGroupMembershipStatus(changedUserGroup.getGroupMembershipStatus());
                            userGroupFromFull.group.setPendingMemberCount(changedUserGroup.group.getPendingMemberCount());
                            break;
                        }
                    }
                } else if (membershipStatus.equals("I")) {

                    changedUserGroup.setGroupMembershipStatus("I");
                    changedUserGroup.group.setActiveMemberCount(activeMemberCount - 1);
                    for (UserGroupModel userGroupFromFull : userGroupsFull) {
                        if (userGroupFromFull.group.getGroupId() == changedUserGroup.group.getGroupId()) {
                            userGroupFromFull.setGroupMembershipStatus(changedUserGroup.getGroupMembershipStatus());
                            userGroupFromFull.group.setActiveMemberCount(changedUserGroup.group.getActiveMemberCount());
                            break;
                        }
                    }
                }

                adapter = new HomeGroupsCellAdapter(getActivity(), getListView(), userGroups, userGroupsFull);
                setListAdapter(adapter);
            }
        }
    }

    @Override
    public void postUserGroups(List<UserGroupModel> userGroups) {

        // Dismiss progress dialog
        if (progressDialog != null)
            progressDialog.dismiss();

        if (userGroups != null && userGroups.size() > 0) {
            Gson gson = new Gson();
            String userGroupsJson = gson.toJson(userGroups);
            this.userGroups = gson.fromJson(userGroupsJson, new TypeToken<List<UserGroupModel>>() {
            }.getType());
            this.userGroupsFull = gson.fromJson(userGroupsJson, new TypeToken<List<UserGroupModel>>() {
            }.getType());
            adapter = new HomeGroupsCellAdapter(getActivity(), getListView(), userGroups, userGroupsFull);
            setListAdapter(adapter);
        } else {
            Toast.makeText(this.getContext(), R.string.msg_no_group, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void backGroundErrorHandler(Exception ex) {

        // Dismiss progress dialog
        if (progressDialog != null)
            progressDialog.dismiss();

        // Print stack trace...may be add logging in future releases
        ex.printStackTrace();

        // display error message
        Toast.makeText(this.getContext(), R.string.error_something_wrong, Toast.LENGTH_SHORT).show();
    }

    /*
        Helper class for implementing the OnRefreshListener for swipe refresh layout and
        IAsycGroupResponse to handle the refresh request.
     */
    private class SwipeRefreshLayoutListener implements SwipeRefreshLayout.OnRefreshListener, IAsyncGroupResponse {

        @Override
        public void postUserGroups(List<UserGroupModel> refreshedGroups) {
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }

            if (refreshedGroups != null && refreshedGroups.size() > 0) {
                Gson gson = new Gson();
                String userGroupsJson = gson.toJson(refreshedGroups);
                userGroups = gson.fromJson(userGroupsJson, new TypeToken<List<UserGroupModel>>() {
                }.getType());
                userGroupsFull = gson.fromJson(userGroupsJson, new TypeToken<List<UserGroupModel>>() {
                }.getType());
                adapter = new HomeGroupsCellAdapter(getActivity(), getListView(), userGroups, userGroupsFull);
                setListAdapter(adapter);
            } else {
                Toast.makeText(getContext(), R.string.msg_no_group, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void backGroundErrorHandler(Exception ex) {
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            // Print stack trace...may be add logging in future releases
            ex.printStackTrace();

            // display error message
            Toast.makeText(getContext(), R.string.error_something_wrong, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRefresh() {
            // Reload groups
            UserService userService = new UserService();
            userService.getUserGroupsByUserId(authenticatedUser.getUserId(), this);
        }
    }
}
