/*
 * Copyright (c) 2015 PocketHub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pockethub.android.ui.repo;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;

import com.alorma.github.sdk.bean.dto.response.Contributor;
import com.alorma.github.sdk.bean.dto.response.Repo;
import com.alorma.github.sdk.bean.dto.response.User;
import com.alorma.github.sdk.services.repo.GetRepoContributorsClient;
import com.alorma.github.sdk.services.user.RequestUserClient;
import com.github.kevinsawicki.wishlist.SingleTypeAdapter;
import com.github.pockethub.android.R;
import com.github.pockethub.android.ThrowableLoader;
import com.github.pockethub.android.accounts.AccountUtils;
import com.github.pockethub.android.rx.ObserverAdapter;
import com.github.pockethub.android.ui.ItemListFragment;
import com.github.pockethub.android.ui.user.UserViewActivity;
import com.github.pockethub.android.util.AvatarLoader;
import com.github.pockethub.android.util.InfoUtils;
import com.google.inject.Inject;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.github.pockethub.android.Intents.EXTRA_REPOSITORY;

/**
 * Fragment to display a list of contributors for a specific repository
 */
public class RepositoryContributorsFragment extends ItemListFragment<Contributor> {

    /**
     * Avatar loader
     */
    @Inject
    protected AvatarLoader avatars;

    private Repo repo;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        repo = getParcelableExtra(EXTRA_REPOSITORY);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(R.string.no_contributors);
    }

    @Override
    public Loader<List<Contributor>> onCreateLoader(int id, Bundle args) {
        return new ThrowableLoader<List<Contributor>>(getActivity(), items) {

            @Override
            public List<Contributor> loadData() throws Exception {
                return new GetRepoContributorsClient(InfoUtils.createRepoInfo(repo)).observable().toBlocking().first();
            }
        };
    }

    @Override
    protected SingleTypeAdapter<Contributor> createAdapter(List<Contributor> items) {
        return new ContributorListAdapter(getActivity(),
                items.toArray(new Contributor[items.size()]), avatars);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final Contributor contributor = (Contributor) l.getItemAtPosition(position);
        new RequestUserClient(contributor.author.login)
                .observable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<User>bindToLifecycle())
                .subscribe(new ObserverAdapter<User>() {
                    @Override
                    public void onNext(User user) {
                        super.onNext(user);
                        if (!AccountUtils.isUser(getActivity(), user))
                            startActivity(UserViewActivity.createIntent(user));
                    }
                });

    }

    @Override
    protected int getErrorMessage(Exception exception) {
        return R.string.error_contributors_load;
    }
}
