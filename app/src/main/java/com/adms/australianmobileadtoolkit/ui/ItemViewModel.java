package com.adms.australianmobileadtoolkit.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ItemViewModel extends ViewModel {
   private final MutableLiveData<Boolean> toggleStatus = new MutableLiveData<Boolean>();
   public void setToggleStatusInViewModel(Boolean item) {
      toggleStatus.setValue(item);
   }
   public LiveData<Boolean> getToggleStatusInViewModel() {
      return toggleStatus;
   }
}