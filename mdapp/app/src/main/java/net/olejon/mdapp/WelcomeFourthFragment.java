package net.olejon.mdapp;

/*

Copyright 2017 Ole Jon Bjørkum

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see http://www.gnu.org/licenses/.

*/

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

public class WelcomeFourthFragment extends Fragment
{
	private Activity mActivity;

	private MyTools mTools;

	private boolean mViewIsShown = false;

	// Create fragment view
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mActivity = getActivity();

		mTools = new MyTools(mActivity);

		ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_welcome_fourth, container, false);

		if(!mViewIsShown)
		{
			TextView textView = viewGroup.findViewById(R.id.welcome_page_4_guide);
			animateTextView(textView);
		}

		Button button = viewGroup.findViewById(R.id.welcome_page_4_button);

		button.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				mTools.setSharedPreferencesBoolean("WELCOME_ACTIVITY_HAS_BEEN_SHOWN", true);

				mActivity.finish();
				mActivity.overridePendingTransition(0, R.anim.welcome_finish);
			}
		});

		return viewGroup;
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser)
	{
		super.setUserVisibleHint(isVisibleToUser);

		if(getView() == null)
		{
			mViewIsShown = false;
		}
		else
		{
			mViewIsShown = true;

			TextView textView = getView().getRootView().findViewById(R.id.welcome_page_4_guide);
			animateTextView(textView);
		}
	}

	private void animateTextView(TextView textView)
	{
		textView.setVisibility(View.VISIBLE);
		textView.startAnimation(AnimationUtils.loadAnimation(mActivity, R.anim.welcome_guide));
	}
}