/*
 * Piwigo for Android
 * Copyright (C) 2016-2018 Piwigo Team http://piwigo.org
 * Copyright (C) 2018-2018 Raphael Mack http://www.raphael-mack.de
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.piwigo.ui.main;

import android.accounts.Account;
import android.content.res.Resources;
import android.util.Log;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableBoolean;
import androidx.lifecycle.ViewModel;

import org.piwigo.BR;
import org.piwigo.R;
import org.piwigo.accounts.UserManager;
import org.piwigo.data.model.Category;
import org.piwigo.data.model.Image;
import org.piwigo.data.repository.CategoriesRepository;
import org.piwigo.data.repository.ImageRepository;
import org.piwigo.io.repository.PreferencesRepository;
import org.piwigo.ui.shared.BindingRecyclerViewAdapter;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;

import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;

/*
import rx.Subscriber;
import rx.Subscription;
*/
public class AlbumsViewModel extends ViewModel {

    private static final String TAG = AlbumsViewModel.class.getName();

    public ObservableBoolean isLoading = new ObservableBoolean();

    public ObservableArrayList<Image> images = new ObservableArrayList<>();
    public ObservableArrayList<Category> albums = new ObservableArrayList<>();
    public BindingRecyclerViewAdapter.ViewBinder<Category> albumsViewBinder = new CategoryViewBinder();
    public BindingRecyclerViewAdapter.ViewBinder<Image> photoViewBinder = new ImagesViewBinder();

    private final UserManager userManager;
    private final CategoriesRepository categoriesRepository;
    private final ImageRepository imageRepository;

    private final Resources resources;

    private Subscription albumsSubscription;
    private Subscription photosSubscription;

    private Integer category = null;

    AlbumsViewModel(UserManager userManager, CategoriesRepository categoriesRepository,
                    ImageRepository imageRepository, Resources resources) {
        this.userManager = userManager;
        this.categoriesRepository = categoriesRepository;
        this.imageRepository = imageRepository;
        this.resources = resources;
    }

    @Override
    protected void onCleared() {
        if (albumsSubscription != null) {
// TODO            albumsSubscription.unsubscribe();
        }
    }

    /* which category is shown by this viewmodel */
    public Integer getCategory() {
        return category;
    }

    private void forcedLoadAlbums(){
        Account account = userManager.getActiveAccount().getValue();
        if (albumsSubscription != null) {
            // cleanup, just in case
//            albumsSubscription.unsubscribe();
            albumsSubscription = null;
        }
        if (photosSubscription != null) {
            // cleanup, just in case
//            photosSubscription.unsubscribe();
            photosSubscription = null;
        }
        if (account != null) {
            categoriesRepository.getCategories(category)
                    .subscribe(new CategoriesSubscriber());
            imageRepository.getImages(category)
                    .subscribe(new ImageSubscriber());
        }
    }

    void loadAlbums(Integer categoryId) {
        if(category == null || category != category) {
            category = categoryId;
            forcedLoadAlbums();
        }
    }

    public void onRefresh() {
        isLoading.set(true);
        forcedLoadAlbums();
    }

    private class CategoriesSubscriber extends DisposableObserver<Category> {

        @Override
        public void onComplete() {

        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof IOException) {
                Log.e(TAG, "CategoriesSubscriber: " + e.getMessage());
                // TODO: #91 tell the user about the network problem
            } else {
                // NO: NEVER throw an exception here
                // throw new RuntimeException(e);
                Log.e(TAG, "CategoriesSubscriber: " + e.getMessage());
                // TODO: #161 highlight problem to the user
            }
        }

        @Override
        public void onNext(Category category) {
            albums.add(category);
        }
    }

    private class CategoryViewBinder implements BindingRecyclerViewAdapter.ViewBinder<Category> {

        @Override
        public int getViewType(Category category) {
            return 0;
        }

        @Override
        public int getLayout(int viewType) {
            return R.layout.item_album;
        }

        @Override
        public void bind(BindingRecyclerViewAdapter.ViewHolder viewHolder, Category category) {
            String photos = resources.getQuantityString(R.plurals.album_photos, category.nbImages, category.nbImages);
            if (category.totalNbImages > category.nbImages) {
                int subPhotos = category.totalNbImages - category.nbImages;
                photos += resources.getQuantityString(R.plurals.album_photos_subs, subPhotos, subPhotos);
            }
            AlbumItemViewModel viewModel = new AlbumItemViewModel(category.thumbnailUrl, category.name, photos, category.id);
            viewHolder.getBinding().setVariable(BR.viewModel, viewModel);
        }
    }

    private class ImageSubscriber extends DisposableObserver<Image>{
        //private class ImagesSubscriber implements Subscriber<Image> {
        @Override
        public void onNext(Image image) {
            images.add(image);
        }

        @Override
        public void onComplete() {

        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof IOException) {
                Log.e(TAG, "ImagesSubscriber: " + e.getMessage());
// TODO: #91 tell the user about the network problem
            } else {
                // NO: NEVER throw an exception here
                // throw new RuntimeException(e);
                Log.e(TAG, "ImagesSubscriber: " + e.getMessage());
                // TODO: #161 highlight problem to the user
            }
        }
    }

    private class ImagesViewBinder implements BindingRecyclerViewAdapter.ViewBinder<Image> {

        @Override
        public int getViewType(Image image) {
            return 0;
        }

        @Override
        public int getLayout(int viewType) {
            return R.layout.item_images;
        }


        @Override
        public void bind(BindingRecyclerViewAdapter.ViewHolder viewHolder, Image image) {
            // TODO: make configurable to also show the photo name here
            ImagesItemViewModel viewModel = new ImagesItemViewModel(image, images.indexOf(image), image.name, images);
            viewHolder.getBinding().setVariable(BR.viewModel, viewModel);
        }

    }
}
