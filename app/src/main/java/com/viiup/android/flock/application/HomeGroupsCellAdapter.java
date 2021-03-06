package com.viiup.android.flock.application;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.joanzapata.iconify.Iconify;
import com.viiup.android.flock.helpers.CommonHelper;
import com.viiup.android.flock.models.UserGroupModel;
import com.viiup.android.flock.services.IAsyncRequestResponse;
import com.viiup.android.flock.services.UserService;

import java.util.List;

/**
 * Created by AbdullahMoyeen on 4/12/16.
 */
public class HomeGroupsCellAdapter extends BaseAdapter {

    private static class CellItemsViewHolder {
        ImageView imageViewGroup;
        TextView textViewGroupCategory;
        TextView textViewGroupName;
        TextView textViewGroupMembersCount;
        TextView textViewGroupDescription;
        Switch switchMembership;
    }

    private Context context;
    private ListView listView;
    private List<UserGroupModel> userGroups;
    private List<UserGroupModel> userGroupsFull;
    private CellItemsViewHolder cellItemsViewHolder;
    private ProgressDialog progressDialog;

    HomeGroupsCellAdapter(Context context, ListView listView, List<UserGroupModel> userGroups, List<UserGroupModel> userGroupsFull) {
        this.context = context;
        this.listView = listView;
        this.userGroups = userGroups;
        this.userGroupsFull = userGroupsFull;
    }

    @Override
    public int getCount() {
        return userGroups.size();
    }

    @Override
    public int getViewTypeCount() {
        int count = getCount();
        return count == 0 ? 1 : count;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return userGroups.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final UserGroupModel userGroup = userGroups.get(position);

        if (convertView == null) {

            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.home_groups_cell, null);

            cellItemsViewHolder = new CellItemsViewHolder();
            cellItemsViewHolder.imageViewGroup = (ImageView) convertView.findViewById(R.id.imageViewGroup);
            cellItemsViewHolder.textViewGroupCategory = (TextView) convertView.findViewById(R.id.textViewGroupCategory);
            cellItemsViewHolder.textViewGroupName = (TextView) convertView.findViewById(R.id.textViewGroupName);
            cellItemsViewHolder.textViewGroupMembersCount = (TextView) convertView.findViewById(R.id.textViewGroupMembersCount);
            cellItemsViewHolder.textViewGroupDescription = (TextView) convertView.findViewById(R.id.textViewGroupDescription);
            cellItemsViewHolder.switchMembership = (Switch) convertView.findViewById(R.id.switchMembership);

            convertView.setTag(cellItemsViewHolder);
        } else {
            cellItemsViewHolder = (CellItemsViewHolder) convertView.getTag();
        }

        if (userGroup != null) {

            cellItemsViewHolder.imageViewGroup.setImageDrawable(CommonHelper.getIconDrawableByGroupCategory(this.context, userGroup.group.getGroupCategory()));
            cellItemsViewHolder.textViewGroupCategory.setText(userGroup.group.getGroupCategory());
            cellItemsViewHolder.textViewGroupName.setText(userGroup.group.getGroupName());
            cellItemsViewHolder.textViewGroupMembersCount.setText(Integer.toString(userGroup.group.getActiveMemberCount()) + " joined");
            cellItemsViewHolder.textViewGroupDescription.setText(userGroup.group.getGroupDescription());
            if (userGroup.getGroupMembershipStatus().equals("I")) {
                cellItemsViewHolder.switchMembership.setTextOff(Iconify.compute(context, context.getString(R.string.icon_fa_leave)));
                cellItemsViewHolder.switchMembership.setTextOn(Iconify.compute(context, context.getString(R.string.icon_fa_pending)));
                cellItemsViewHolder.switchMembership.setEnabled(true);
            } else if (userGroup.getGroupMembershipStatus().equals("P")) {
                cellItemsViewHolder.switchMembership.setTextOff(Iconify.compute(context, context.getString(R.string.icon_fa_leave)));
                cellItemsViewHolder.switchMembership.setTextOn(Iconify.compute(context, context.getString(R.string.icon_fa_pending)));
                cellItemsViewHolder.switchMembership.setEnabled(false);
            } else if (userGroup.getGroupMembershipStatus().equals("A")) {
                cellItemsViewHolder.switchMembership.setTextOff(Iconify.compute(context, context.getString(R.string.icon_fa_leave)));
                cellItemsViewHolder.switchMembership.setTextOn(Iconify.compute(context, context.getString(R.string.icon_fa_join)));
                cellItemsViewHolder.switchMembership.setEnabled(true);
            }
            cellItemsViewHolder.switchMembership.setOnCheckedChangeListener(null);
            cellItemsViewHolder.switchMembership.setChecked(!userGroup.getGroupMembershipStatus().equals("I"));
            cellItemsViewHolder.switchMembership.setOnCheckedChangeListener(new SwitchMembershipOnCheckedChangeListener());
        }

        return convertView;
    }

    private class SwitchMembershipOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener, IAsyncRequestResponse {

        private boolean isMember;
        private int position;

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isOn) {

            this.isMember = isOn;
            this.position = listView.getPositionForView(buttonView);

            // Display progress bar
            progressDialog = ProgressDialog.show(context, buttonView.getResources().getString(R.string.title_membership), buttonView.getResources().getString(R.string.msg_processing_request));

            UserService userService = new UserService();
            userService.setUserGroupMembership(userGroups.get(position).getUserId(), userGroups.get(position).group.getGroupId(), isOn, this);
        }

        @Override
        public void responseHandler(String response) {

            if (progressDialog != null) progressDialog.dismiss();

            if (response.equalsIgnoreCase("200")) {

                UserGroupModel changedUserGroup = userGroups.get(position);
                int pendingMemberCount = changedUserGroup.group.getPendingMemberCount();
                int activeMemberCount = changedUserGroup.group.getActiveMemberCount();

                if (isMember) {

                    changedUserGroup.setGroupMembershipStatus("P");
                    changedUserGroup.group.setPendingMemberCount(pendingMemberCount + 1);
                    for (UserGroupModel userGroupFromFull : userGroupsFull) {
                        if (userGroupFromFull.group.getGroupId() == changedUserGroup.group.getGroupId()) {
                            userGroupFromFull.setGroupMembershipStatus(changedUserGroup.getGroupMembershipStatus());
                            userGroupFromFull.group.setPendingMemberCount(changedUserGroup.group.getPendingMemberCount());
                            break;
                        }
                    }
                    cellItemsViewHolder.switchMembership.setEnabled(false);
                    Toast.makeText(context, R.string.msg_join_request_sent, Toast.LENGTH_SHORT).show();
                } else {

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

                listView.setAdapter(listView.getAdapter());
            } else {
                Toast.makeText(context, R.string.msg_processing_failed, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void backGroundErrorHandler(Exception ex) {

            if (progressDialog != null) progressDialog.dismiss();

            // Print stack trace...may be add logging in future releases
            ex.printStackTrace();

            // display error message
            Toast.makeText(context, R.string.error_something_wrong, Toast.LENGTH_SHORT).show();
        }
    }
}
