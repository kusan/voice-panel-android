/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thanksmister.iot.voicepanel.ui.fragments

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.thanksmister.iot.voicepanel.BaseFragment
import com.thanksmister.iot.voicepanel.R
import com.thanksmister.iot.voicepanel.persistence.Configuration
import com.thanksmister.iot.voicepanel.persistence.IntentMessage
import com.thanksmister.iot.voicepanel.persistence.MessageMqtt
import com.thanksmister.iot.voicepanel.ui.adapters.CommandAdapter
import com.thanksmister.iot.voicepanel.ui.adapters.MessageAdapter
import com.thanksmister.iot.voicepanel.ui.viewmodels.LogsViewModel
import com.thanksmister.iot.voicepanel.utils.DialogUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_logs.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class LogFragment : BaseFragment() {

    @Inject
    lateinit var dialogUtils: DialogUtils

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: LogsViewModel

    override fun onAttach(context: Context?) {
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true);
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(LogsViewModel::class.java)
        observeViewModel(viewModel)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_logs, menu)
        val itemLen = menu.size()
        for (i in 0 until itemLen) {
            val drawable = menu.getItem(i).icon
            if (drawable != null) {
                drawable.mutate()
                drawable.setColorFilter(resources.getColor(R.color.gray), PorterDuff.Mode.SRC_ATOP)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            onBackPressed()
            return true
        } else if (id == R.id.action_delete) {
            clearMessages()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view is RecyclerView) {
            val context = view.getContext()
            view.layoutManager = LinearLayoutManager(context)
            view.adapter =  MessageAdapter(ArrayList<MessageMqtt>())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_logs, container, false)
    }

    override fun onDetach() {
        super.onDetach()
        disposable.dispose()
    }

    private fun clearMessages() {
        if(isAdded && activity != null) {
            dialogUtils.showAlertDialogCancel(activity!!, getString(R.string.dialog_clear_logs), DialogInterface.OnClickListener { _, _ ->
                disposable.add(viewModel.clearMessages()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            logs_list.adapter = MessageAdapter(ArrayList<MessageMqtt>())
                            logs_list.invalidate()
                        }, { error -> Timber.e("Unable to delete messages: " + error)}))
            })
        }
    }

    private fun observeViewModel(viewModel: LogsViewModel) {
        Timber.d("observeViewModel")
        disposable.add(viewModel.getMessages()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({messages ->
                    Timber.d("messages: " + messages)
                    logs_list.adapter = MessageAdapter(messages)
                    logs_list.invalidate()
                }, { error -> Timber.e("Unable to get messages: " + error)}))
    }

    companion object {
        fun newInstance(): LogFragment {
            return LogFragment()
        }
    }
}