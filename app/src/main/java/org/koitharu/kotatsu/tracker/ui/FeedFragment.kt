package org.koitharu.kotatsu.tracker.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_tracklogs.*
import org.koin.android.ext.android.get
import org.koin.android.viewmodel.ext.android.viewModel
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.list.PaginationScrollListener
import org.koitharu.kotatsu.base.ui.list.decor.SpacingItemDecoration
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.tracker.work.TrackWorker
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.hasItems

class FeedFragment : BaseFragment(R.layout.fragment_tracklogs), PaginationScrollListener.Callback,
	OnListItemClickListener<Manga> {

	private val viewModel by viewModel<FeedViewModel>()

	private var adapter: FeedAdapter? = null

	override fun getTitle() = context?.getString(R.string.updates)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		adapter = FeedAdapter(get(), this)
		recyclerView.adapter = adapter
		recyclerView.addItemDecoration(
			SpacingItemDecoration(resources.getDimensionPixelOffset(R.dimen.grid_spacing))
		)
		recyclerView.setHasFixedSize(true)
		recyclerView.addOnScrollListener(PaginationScrollListener(4, this))

		viewModel.content.observe(viewLifecycleOwner, this::onListChanged)
		viewModel.isLoading.observe(viewLifecycleOwner, this::onLoadingStateChanged)
		viewModel.onError.observe(viewLifecycleOwner, this::onError)
		viewModel.isEmptyState.observe(viewLifecycleOwner, this::onEmptyStateChanged)
	}

	override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
		super.onCreateOptionsMenu(menu, inflater)
		inflater.inflate(R.menu.opt_feed, menu)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_update -> {
			TrackWorker.startNow(requireContext())
			Snackbar.make(recyclerView, R.string.feed_will_update_soon, Snackbar.LENGTH_LONG).show()
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onDestroyView() {
		adapter = null
		super.onDestroyView()
	}

	private fun onListChanged(list: List<Any>) {
		adapter?.items = list
	}

	private fun onError(e: Throwable) {
		if (recyclerView.hasItems) {
			Snackbar.make(recyclerView, e.getDisplayMessage(resources), Snackbar.LENGTH_SHORT)
				.show()
		} else {
			textView_holder.text = e.getDisplayMessage(resources)
			textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(
				0,
				R.drawable.ic_error_large,
				0,
				0
			)
			layout_holder.isVisible = true
		}
	}

	private fun onLoadingStateChanged(isLoading: Boolean) {
		val hasItems = recyclerView.hasItems
		progressBar.isVisible = isLoading && !hasItems
	}

	private fun onEmptyStateChanged(isEmpty: Boolean) {
		if (isEmpty) {
			setUpEmptyListHolder()
		}
		layout_holder.isVisible = isEmpty
	}

	override fun onScrolledToEnd() {
		viewModel.loadList(append = true)
	}

	override fun onItemClick(item: Manga, view: View) {
		startActivity(DetailsActivity.newIntent(context ?: return, item))
	}

	private fun setUpEmptyListHolder() {
		textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
		textView_holder.setText(R.string.text_feed_holder)
	}

	companion object {

		fun newInstance() = FeedFragment()
	}
}