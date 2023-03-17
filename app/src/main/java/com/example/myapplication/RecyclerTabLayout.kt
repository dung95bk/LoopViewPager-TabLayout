package com.example.myapplication

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.size
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs

class RecyclerTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {
    private var mIndicatorPaint: Paint
    private var mTabBackgroundResId = 0
    private var mTabIndicatorResId = 0
    private var mTabIndicatorDrawable: Drawable? = null
    private var mTabOnScreenLimit = 0
    private var mTabMinWidth = 0
    private var mTabMaxWidth = 0
    private var mTabTextAppearance = 0
    private var mTabSelectedTextColor = 0
    private var mTabTextColor = 0
    private var mTabSelectedTextColorSet = false
    private var mTabPaddingStart = 0
    private var mTabPaddingTop = 0
    private var mTabPaddingEnd = 0
    private var mTabPaddingBottom = 0

    private var mTabMarginStart = 0
    private var mTabMarginTop = 0
    private var mTabMarginEnd = 0
    private var mTabMarginBottom = 0

    private var mIndicatorHeight = 0
    private var mLinearLayoutManager: LinearLayoutManager
    private var mViewPager: ViewPager? = null
    private var onPageListener: ViewPagerOnPageChangeListener? = null
    private var mAdapter: Adapter<*>? = null
    private var mIndicatorPosition = 0
    private var mIndicatorGap = 0
    private var mIndicatorScroll = 0
    private var mOldPosition = 0
    private var mOldScrollOffset = 0
    private var mOldPositionOffset = 0f
    private var mPositionThreshold: Float
    private var mCurrentOffset = 0f
    private var mRequestScrollToTab = false
    private var mScrollEanbled = false
    private var isInfinityLoop = false
    private var itemTitle: List<String> = arrayListOf()
    var lockPageChangeListener: Boolean = false

    init {
        setWillNotDraw(false)
        mIndicatorPaint = Paint()
        getAttributes(context, attrs, defStyle)
        mLinearLayoutManager = LinearLayoutManager(getContext())
        mLinearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        layoutManager = mLinearLayoutManager
        itemAnimator = null
        mPositionThreshold = DEFAULT_POSITION_THRESHOLD
    }

    fun setPaddingStart(paddingStart: Int) {
        mTabPaddingStart = paddingStart
    }

    fun setPaddingHorizontal(paddingHorizontal: Int) {
        mTabPaddingStart = paddingHorizontal
        mTabPaddingEnd = paddingHorizontal
    }

    fun setPaddingEnd(paddingEnd: Int) {
        mTabPaddingEnd = paddingEnd
    }

    fun setInfinityLoop(isLoop: Boolean = false) {
        this.isInfinityLoop = isLoop
    }

    fun setTabIndicatorDrawable(resId: Int) {
        mTabIndicatorResId = resId
        mTabIndicatorDrawable = ResourcesCompat.getDrawable(resources, mTabIndicatorResId, null)
    }

    @SuppressLint("CustomViewStyleable")
    private fun getAttributes(context: Context, attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.rtl_RecyclerTabLayout, defStyle, R.style.rtl_RecyclerTabLayout
        )
        setIndicatorColor(a.getColor(R.styleable.rtl_RecyclerTabLayout_rtl_tabIndicatorColor, 0))
        setIndicatorHeight(
            a.getDimensionPixelSize(
                R.styleable.rtl_RecyclerTabLayout_rtl_tabIndicatorHeight, 0
            )
        )
        mTabTextAppearance = a.getResourceId(
            R.styleable.rtl_RecyclerTabLayout_rtl_tabTextAppearance,
            R.style.rtl_RecyclerTabLayout_Tab
        )
        mTabPaddingBottom =
            a.getDimensionPixelSize(R.styleable.rtl_RecyclerTabLayout_rtl_tabPadding, 0)
        mTabPaddingEnd = mTabPaddingBottom
        mTabPaddingTop = mTabPaddingEnd
        mTabPaddingStart = mTabPaddingTop
        mTabPaddingStart = a.getDimensionPixelSize(
            R.styleable.rtl_RecyclerTabLayout_rtl_tabPaddingStart, mTabPaddingStart
        )
        mTabPaddingTop = a.getDimensionPixelSize(
            R.styleable.rtl_RecyclerTabLayout_rtl_tabPaddingTop, mTabPaddingTop
        )
        mTabPaddingEnd = a.getDimensionPixelSize(
            R.styleable.rtl_RecyclerTabLayout_rtl_tabPaddingEnd, mTabPaddingEnd
        )
        mTabPaddingBottom = a.getDimensionPixelSize(
            R.styleable.rtl_RecyclerTabLayout_rtl_tabPaddingBottom, mTabPaddingBottom
        )
        if (a.hasValue(R.styleable.rtl_RecyclerTabLayout_rtl_tabSelectedTextColor)) {
            mTabSelectedTextColor =
                a.getColor(R.styleable.rtl_RecyclerTabLayout_rtl_tabSelectedTextColor, 0)
            mTabSelectedTextColorSet = true
        }
        mTabOnScreenLimit = a.getInteger(
            R.styleable.rtl_RecyclerTabLayout_rtl_tabOnScreenLimit, 0
        )
        if (mTabOnScreenLimit == 0) {
            mTabMinWidth = a.getDimensionPixelSize(
                R.styleable.rtl_RecyclerTabLayout_rtl_tabMinWidth, 0
            )
            mTabMaxWidth = a.getDimensionPixelSize(
                R.styleable.rtl_RecyclerTabLayout_rtl_tabMaxWidth, 0
            )
        }
        mTabBackgroundResId =
            a.getResourceId(R.styleable.rtl_RecyclerTabLayout_rtl_tabBackground, 0)
        mTabIndicatorResId = 0
        mTabIndicatorDrawable = null
        mScrollEanbled = a.getBoolean(R.styleable.rtl_RecyclerTabLayout_rtl_scrollEnabled, true)
        a.recycle()
    }

    fun setIndicatorColor(color: Int) {
        mIndicatorPaint.color = color
    }

    fun setIndicatorHeight(indicatorHeight: Int) {
        mIndicatorHeight = indicatorHeight
    }

    fun setPositionThreshold(positionThreshold: Float) {
        mPositionThreshold = positionThreshold
    }

    fun setItemsTitle(itemTitle: List<String>) {
        this.itemTitle = itemTitle
    }

    fun setUpWithViewPager(viewPager: ViewPager?) {
        val adapter = DefaultAdapter(viewPager, itemTitle, {
            mIndicatorPosition =
                (it / itemTitle.size) * itemTitle.size + (mIndicatorPosition % itemTitle.size)
            onPageListener?.lastIndexTab = mIndicatorPosition
            scrollToTab(mIndicatorPosition)
            mIndicatorPosition
        }, {
            onPageListener?.mScrollState ?: ViewPager.SCROLL_STATE_IDLE
        }) {
            lockPageChangeListener = false
        }
        lockPageChangeListener = true
        adapter.setTabPadding(
            mTabPaddingStart, mTabPaddingTop, mTabPaddingEnd, mTabPaddingBottom
        )
        adapter.setIsLoop(isInfinityLoop)
        adapter.setMargin(mTabMarginStart, mTabMarginTop, mTabMarginEnd, mTabMarginBottom)
        adapter.setTabTextAppearance(mTabTextAppearance)
        adapter.setTabSelectedTextColor(mTabSelectedTextColorSet, mTabSelectedTextColor)
        adapter.setTabMaxWidth(mTabMaxWidth)
        adapter.setTabMinWidth(mTabMinWidth)
        adapter.setTabBackgroundResId(mTabBackgroundResId)
        adapter.setTabOnScreenLimit(10)
//        if (isInfinityLoop.not()) {
//            mLinearLayoutManager = NoScrollLinearLayoutManager(context)
//        }
        layoutManager = mLinearLayoutManager
        setUpWithAdapter(adapter)
    }

    class NoScrollLinearLayoutManager(context: Context) : LinearLayoutManager(context) {
        override fun canScrollHorizontally(): Boolean {
            return false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpWithAdapter(adapter: Adapter<*>) {
        mAdapter = adapter
        mViewPager = adapter.viewPager
        requireNotNull(mViewPager!!.adapter) { "ViewPager does not have a PagerAdapter set" }
        val tarPosition = if (isInfinityLoop) {
            (Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2 % itemTitle.size)) + getTabIndexFromViewPagerIndex()
        } else {
            mViewPager!!.currentItem - 1
        }
        onPageListener = ViewPagerOnPageChangeListener(
            this, tarPosition, mViewPager!!.currentItem
        )
        mViewPager?.addOnPageChangeListener(
            onPageListener!!
        )
        setAdapter(adapter)
        scrollToTab(tarPosition)
    }

    fun getTabIndexFromViewPagerIndex(): Int {
        mViewPager!!.currentItem.let {
            val totalSize = mViewPager!!.size
            if (it == totalSize - 1) {
                return 0
            }
            if (it == 0) {
                return itemTitle.size - 1
            }
            return it - 1
        }
    }

    override fun onDetachedFromWindow() {
        mViewPager?.clearOnPageChangeListeners()
        onPageListener = null
        super.onDetachedFromWindow()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun scrollToTab(position: Int) {
        scrollToTab(position, 0f)
        mAdapter?.currentIndicatorPosition = position
    }

    private fun scrollToTab(position: Int, positionOffset: Float) {
        if (isInfinityLoop) {
            var scrollOffset = 0
            // lấy view đang chọn
            val selectedView = mLinearLayoutManager.findViewByPosition(position)
            // view kế tiếp
            val nextView = mLinearLayoutManager.findViewByPosition(position + 1)
            if (selectedView != null) {
                // kích thước tablayout
                val tabLayoutSize = measuredWidth
                // padding left view đang chọn
                val sLeft: Float =
                    if (position == 0) 0F else tabLayoutSize / 2f - selectedView.measuredWidth / 2f // left edge of selected tab
                // padding rign view đang chọn
                val sRight = sLeft + selectedView.measuredWidth // right edge of selected tab
                // di chuyển sang phải
                if (nextView != null) {
                    // padding left nextview
                    val nLeft =
                        tabLayoutSize / 2f - nextView.measuredWidth / 2f // left edge of next tab
                    // Tổng khoảng cách cần di chuyển indicatior == padding phải của view trước - padding trái của view sau
                    val distance =
                        sRight - nLeft // total distance that is needed to distance to next tab
                    // khaongr cách cần di chuyển ở điểm hiện tại
                    val dx = distance * positionOffset
                    //
                    scrollOffset = (sLeft - dx).toInt()
                    if (position == 0) {
                        val indicatorGap =
                            ((nextView.measuredWidth - selectedView.measuredWidth) / 2).toFloat()
                        mIndicatorGap = (indicatorGap * positionOffset).toInt()
                        mIndicatorScroll =
                            ((selectedView.measuredWidth + indicatorGap) * positionOffset).toInt()
                    } else {
                        val indicatorGap =
                            ((nextView.measuredWidth - selectedView.measuredWidth) / 2).toFloat()
                        mIndicatorGap = (indicatorGap * positionOffset).toInt()
                        mIndicatorScroll = dx.toInt()
                    }
                } else {
                    // di chuyển sang trai
                    scrollOffset = sLeft.toInt()
                    mIndicatorScroll = 0
                    mIndicatorGap = 0
                }
            } else {
                if (measuredWidth > 0 && mTabMaxWidth > 0 && mTabMinWidth == mTabMaxWidth) { // fixed size
                    val width = mTabMinWidth
                    val offset = (positionOffset * -width).toInt()
                    val leftOffset = ((measuredWidth - width) / 2f).toInt()
                    scrollOffset = offset + leftOffset
                }
                mRequestScrollToTab = true
            }
            updateCurrentIndicatorPosition(
                position, positionOffset - mOldPositionOffset, positionOffset
            )
            mIndicatorPosition = position
            stopScroll()
            if (position != mOldPosition || scrollOffset != mOldScrollOffset) {

                mLinearLayoutManager.scrollToPositionWithOffset(position, scrollOffset)
            }
            if (mIndicatorHeight > 0) {
                invalidate()
            }
            mOldPosition = position
            mOldScrollOffset = scrollOffset
            mOldPositionOffset = positionOffset

        } else {
            if (position <= -1 || position >= itemTitle.size) return
            var scrollOffset = 0
            // lấy view đang chọn
            val selectedView = mLinearLayoutManager.findViewByPosition(position)
            // view kế tiếp
            val nextView = mLinearLayoutManager.findViewByPosition(position + 1)
            if (selectedView != null) {
                // kích thước tablayout
                val tabLayoutSize = measuredWidth
                // Nếu phần position =0  thì sLeft là 0, sRight = kích thước item
                //Nếu ko phải thì sLeft tính theo trung tâm tablayout, sleft = tâm giữa - 1 nửa width item, sright = tâm giữa + 1 nửa
                val sLeft: Float =
                    tabLayoutSize / 2f - selectedView.measuredWidth / 2f // left edge of selected tab
                // padding rign view đang chọn
                val sRight = sLeft + selectedView.measuredWidth // right edge of selected tab
                Log.e("SlectedView", "position:${position}/sLeft:$sLeft/sRight:$sRight")
                // di chuyển sang phải
                if (nextView != null) {
                    // padding left nextview
                    //sLeft tính theo tâm giữa + 1/2 width item
                    val nLeft =
                        tabLayoutSize / 2f - nextView.measuredWidth / 2f // left edge of next tab
                    // Tổng khoảng cách cần di chuyển indicatior == padding phải của view trước - padding trái của view sau
                    //Khaongr cách phải đi = 1/2 next item + 1/2 next view

                    val distance =
                        sRight - nLeft // total distance that is needed to distance to next tab
                    Log.e("NextView", "position:${position}/nLeft:$nLeft/distance: $distance")

                    // khaongr cách giữa tâm 2 item
                    val dx = distance * positionOffset

                    //
                    scrollOffset = (sLeft - sRight + nLeft).toInt()
                    //Tổng Chenh lệch giữa 1 nửa chiều rộng 2 item
                    val indicatorGap =
                        ((nextView.measuredWidth - selectedView.measuredWidth) / 2).toFloat()
                    // Giá trị biến thiên
                    mIndicatorGap = (indicatorGap * positionOffset).toInt()
                    //Khoảng cách tâm 2 item
                    mIndicatorScroll = dx.toInt()
                    Log.e("Final", "dx:$dx/scrollOffset:$scrollOffset/indicatorGap:$indicatorGap/mIndicatorGap:$mIndicatorGap/mIndicatorScroll:$mIndicatorScroll")
                    //view!!.left +  ((nextView.measuredWidth +selectedView.measuredWidth) / 2).) * positionOffset -  ((nextView.measuredWidth - selectedView.measuredWidth) / 2).toFloat()* positionOffset
                // view!!.left + ((nextView.measuredWidth +selectedView.measuredWidth) / 2).) * positionOffset +  ((nextView.measuredWidth - selectedView.measuredWidth) / 2).toFloat()* positionOffset
                } else {
                    // di chuyển sang trai
                    val firstView = mLinearLayoutManager.findViewByPosition(0)
                    if (firstView != null && positionOffset != 0f) {
                        mIndicatorGap = ((-selectedView.measuredWidth + firstView.measuredWidth)/2*positionOffset).toInt()
                        mIndicatorScroll = (((selectedView.measuredWidth + firstView.measuredWidth)/2).toInt() * positionOffset).toInt()
//                        Log.e("All way not found", "mIndicatorGap:$mIndicatorGap, mIndicatorScroll: $mIndicatorScroll")

                    } else {
//                        Log.e("All way not found-1","" )

                        scrollOffset = sLeft.toInt()
                        mIndicatorScroll = 0
                        mIndicatorGap = 0
                    }
                }
            } else {
                if (measuredWidth > 0 && mTabMaxWidth > 0 && mTabMinWidth == mTabMaxWidth) { // fixed size
                    val width = mTabMinWidth
                    val offset = (positionOffset * -width).toInt()
                    val leftOffset = ((measuredWidth - width) / 2f).toInt()
                    scrollOffset = offset + leftOffset
                }
                mRequestScrollToTab = true
            }
            updateCurrentIndicatorPosition(
                position, positionOffset - mOldPositionOffset, positionOffset
            )
            mIndicatorPosition = position
            stopScroll()
            if (position != mOldPosition || scrollOffset != mOldScrollOffset) {

                mLinearLayoutManager.scrollToPositionWithOffset(position, scrollOffset)
            }
            if (mIndicatorHeight > 0) {
                invalidate()
            }
            mOldPosition = position
            mOldScrollOffset = scrollOffset
            mOldPositionOffset = positionOffset

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateCurrentIndicatorPosition(position: Int, dx: Float, positionOffset: Float) {
        if (mAdapter == null) {
            return
        }
        if (isInfinityLoop) {
            var indicatorPosition = -1
            if (dx > 0 && positionOffset >= mPositionThreshold - POSITION_THRESHOLD_ALLOWABLE) {
                indicatorPosition = position + 1
            } else if (dx < 0 && positionOffset <= 1 - mPositionThreshold + POSITION_THRESHOLD_ALLOWABLE) {
                indicatorPosition = position
            }
            if (indicatorPosition >= 0 && indicatorPosition != mAdapter!!.currentIndicatorPosition) {
                mAdapter?.currentIndicatorPosition = indicatorPosition
                mAdapter?.notifyDataSetChanged()
            }

        } else {
            var indicatorPosition = -1
            if (dx > 0 && positionOffset >= 0.6 - POSITION_THRESHOLD_ALLOWABLE) {
                indicatorPosition = position + 1
            } else if (dx < 0 && positionOffset <= 1 - 0.6 + POSITION_THRESHOLD_ALLOWABLE) {
                indicatorPosition = position
            }
            if (indicatorPosition >= 0 && indicatorPosition != mAdapter!!.currentIndicatorPosition) {
                mAdapter?.currentIndicatorPosition = indicatorPosition
                mAdapter?.notifyDataSetChanged()
            }

        }
    }

    override fun onDraw(canvas: Canvas) {

        lockPageChangeListener = false
        val titleCount = itemTitle.size
        var view = mLinearLayoutManager.findViewByPosition(mIndicatorPosition)
        if (mRequestScrollToTab && mViewPager != null) {
            mRequestScrollToTab = false
            scrollToTab(mIndicatorPosition)
            return
        }
        mRequestScrollToTab = false
        if (view == null && titleCount > 0) {
            val realPosition = mIndicatorPosition % titleCount
            val firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition()
            val lastVisibleItem = mLinearLayoutManager.findLastVisibleItemPosition()
            (firstVisibleItem..lastVisibleItem).forEach {
                if (it % titleCount == realPosition) {
                    view = mLinearLayoutManager.findViewByPosition(it)
                    return@forEach
                }
            }
        }
        if (view == null) return
        val top = height - mIndicatorHeight
        val bottom = height
        val left: Int
        val right: Int

        if (isLayoutRtl) {
            left = view!!.left - mIndicatorScroll - mIndicatorGap
            right = view!!.right - mIndicatorScroll + mIndicatorGap
        } else {
            //lấy view đang chọn có indicator bên dưới
            //tính padding left và right của nó
            // total padding = padding view +
            //Nói chung căn trái = điểm trái của item left + width selecte * offset
            //căn phải =  diểm phải của item + width next item * selectef
            left = view!!.left + mIndicatorScroll - mIndicatorGap
            right = view!!.right + mIndicatorScroll + mIndicatorGap
            Log.e("Drawer", "left:$left, right:$right, measuredWidth: $measuredWidth, ${view!!.width}")
            if (left > measuredWidth - view!!.width && right > measuredWidth) {
                var firstItem = mLinearLayoutManager.findViewByPosition(0)
                if (firstItem != null) {
                    val firstLeft = view!!.left + mIndicatorScroll - mIndicatorGap
                    val firstRight = view!!.right + mIndicatorScroll + mIndicatorGap
                    if (mTabIndicatorDrawable != null) {
                        Log.e("OnDraw", "$left/$right")
                        mTabIndicatorDrawable?.setBounds(0, top, right - measuredWidth, bottom)
                        mTabIndicatorDrawable?.draw(canvas)
                    } else {
                        Log.e("OnDraw", "$firstLeft/$firstRight")
                        canvas.drawRect(
                           0f,
                            top.toFloat(),
                            (right - measuredWidth).toFloat(),
                            bottom.toFloat(),
                            mIndicatorPaint
                        )
                    }
                }
            }
        }

        if (mTabIndicatorDrawable != null) {
            Log.e("OnDraw", "$left/$right")
            mTabIndicatorDrawable?.setBounds(left, top, right, bottom)
            mTabIndicatorDrawable?.draw(canvas)
        } else {
            canvas.drawRect(
                left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), mIndicatorPaint
            )
        }
    }

    private val isLayoutRtl: Boolean
        get() = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL

    inner class ViewPagerOnPageChangeListener(
        private val mRecyclerTabLayout: RecyclerTabLayout,
        var lastIndexTab: Int = 0,
        private var lastIndexPager: Int = 1
    ) : ViewPager.OnPageChangeListener {
        var mScrollState = 0
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            mCurrentOffset = positionOffset
            if (lockPageChangeListener.not()) {
                if (isInfinityLoop) {
                    val targetPosition = if (lastIndexPager == position) {
                        lastIndexTab
                    } else if (position == lastIndexPager - 1) {
                        lastIndexTab - 1
                    } else if (position == lastIndexPager + 1) {
                        lastIndexTab + 1
                    } else {
                        lastIndexTab + (position - lastIndexPager)
                    }
                    mRecyclerTabLayout.scrollToTab(targetPosition, positionOffset)
                } else {
                    val targetPosition = if (lastIndexPager == position) {
                        lastIndexTab
                    } else if (lastIndexPager == 1 && position == 0) {
                        itemTitle.size - 1
                    } else if (lastIndexPager == mViewPager!!.size - 2 && position == mViewPager!!.size - 1) {
                        0
                    } else if (position == lastIndexPager - 1) {
                        lastIndexTab - 1
                    } else if (position == lastIndexPager + 1) {
                        lastIndexTab + 1
                    } else {
                        lastIndexTab + (position - lastIndexPager)
                    }

                    mRecyclerTabLayout.scrollToTab(targetPosition, positionOffset)
                }
            }
        }

        override fun onPageScrollStateChanged(state: Int) {

            mScrollState = state
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                when (mViewPager!!.currentItem) {
                    mViewPager!!.size - 1 -> {
                        if (isInfinityLoop) {
                            lockPageChangeListener = true
                            }
                        mViewPager!!.setCurrentItem(1, false)
                    }
                    0 -> {
                        if (isInfinityLoop) {
                            lockPageChangeListener = true
                        }
                        mViewPager!!.setCurrentItem(mViewPager!!.size - 2, false)
                    }
                }
                lastIndexTab = mRecyclerTabLayout.mIndicatorPosition
                lastIndexPager = mRecyclerTabLayout.mViewPager!!.currentItem
                lockPageChangeListener = false
            }
        }

        override fun onPageSelected(position: Int) {
        }
    }

    abstract class Adapter<T : ViewHolder?>(var viewPager: ViewPager?) : RecyclerView.Adapter<T>() {
        abstract fun setIsLoop(isLoop: Boolean)

        var currentIndicatorPosition = 0
    }

    class DefaultAdapter(
        viewPager: ViewPager?,
        private val itemTitle: List<String>,
        val scrollToPosition: (Int) -> Int,
        val getScrollState: () -> Int,
        val clickTab: () -> Unit
    ) : Adapter<DefaultAdapter.ViewHolder?>(viewPager) {
        private var mTabPaddingStart = 0
        private var mTabPaddingTop = 0
        private var mTabPaddingEnd = 0
        private var mTabPaddingBottom = 0
        private var mTabMarginStart = 0
        private var mTabMarginTop = 0
        private var mTabMarginEnd = 0
        private var mTabMarginBottom = 0
        private var mTabTextAppearance = 0
        private var mTabSelectedTextColorSet = false
        private var mTabSelectedTextColor = 0
        private var mTabMaxWidth = 0
        private var mTabMinWidth = 0
        private var mTabBackgroundResId = 0
        private var mTabOnScreenLimit = 0
        var count = 0
        var isLoop = false
        var itemSize = itemTitle.size
        override fun setIsLoop(isLoop: Boolean) {
            this.isLoop = isLoop
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tabTextView = TabTextView(parent.context)
            ViewCompat.setPaddingRelative(
                tabTextView, mTabPaddingStart, mTabPaddingTop, mTabPaddingEnd, mTabPaddingBottom
            )
            tabTextView.gravity = Gravity.CENTER
            tabTextView.maxLines = MAX_TAB_TEXT_LINES
            tabTextView.ellipsize = TextUtils.TruncateAt.END
            tabTextView.setTextAppearance(tabTextView.context, mTabTextAppearance)
            if (mTabSelectedTextColorSet) {
                tabTextView.setTextColor(
                    tabTextView.createColorStateList(
                        tabTextView.currentTextColor, mTabSelectedTextColor
                    )
                )
            }
            if (mTabBackgroundResId != 0) {
                tabTextView.setBackgroundDrawable(
                    AppCompatResources.getDrawable(tabTextView.context, mTabBackgroundResId)
                )
            }
            tabTextView.layoutParams = createLayoutParamsForTabs()
            return ViewHolder(tabTextView, {
                scrollToPosition.invoke(it)
            }, {
                getScrollState.invoke()
            }) {
                clickTab.invoke()
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.title.text = "${itemTitle[position % itemTitle.size]}"
            holder.title.isSelected =
                currentIndicatorPosition % itemTitle.size == position % itemTitle.size
        }

        override fun getItemCount(): Int {
            return if (isLoop) Int.MAX_VALUE else itemTitle.size
        }

        fun setTabPadding(
            tabPaddingStart: Int,
            tabPaddingTop: Int,
            tabPaddingEnd: Int,
            tabPaddingBottom: Int
        ) {
            mTabPaddingStart = tabPaddingStart
            mTabPaddingTop = tabPaddingTop
            mTabPaddingEnd = tabPaddingEnd
            mTabPaddingBottom = tabPaddingBottom
        }

        fun setMargin(
            tabMarginStart: Int,
            tabMarginTop: Int,
            tabMarginEnd: Int,
            tabMarginBottom: Int
        ) {
            mTabMarginStart = tabMarginStart
            mTabMarginTop = tabMarginTop
            mTabMarginEnd = tabMarginEnd
            mTabMarginBottom = tabMarginBottom
        }

        fun setTabTextAppearance(tabTextAppearance: Int) {
            mTabTextAppearance = tabTextAppearance
        }

        fun setTabSelectedTextColor(
            tabSelectedTextColorSet: Boolean,
            tabSelectedTextColor: Int
        ) {
            mTabSelectedTextColorSet = tabSelectedTextColorSet
            mTabSelectedTextColor = tabSelectedTextColor
        }

        fun setTabMaxWidth(tabMaxWidth: Int) {
            mTabMaxWidth = tabMaxWidth
        }

        fun setTabMinWidth(tabMinWidth: Int) {
            mTabMinWidth = tabMinWidth
        }

        fun setTabBackgroundResId(tabBackgroundResId: Int) {
            mTabBackgroundResId = tabBackgroundResId
        }

        fun setTabOnScreenLimit(tabOnScreenLimit: Int) {
            mTabOnScreenLimit = tabOnScreenLimit
        }

        private fun createLayoutParamsForTabs(): LayoutParams {
            return LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(mTabMarginStart, mTabMarginTop, mTabMarginEnd, mTabMarginBottom)
            }
        }

        inner class ViewHolder(
            itemView: View,
            scrollToPosition: (Int) -> Int,
            getScrollState: () -> Int,
            clickTab: () -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
            var title: TextView

            init {


                title = itemView as TextView
                itemView.setOnClickListener {
                    viewPager?.let { pager ->
                        var lastTabPosition = currentIndicatorPosition
                        val newTabPosition = adapterPosition
                        val isNotSameSection = lastTabPosition % itemSize != newTabPosition % itemSize
                        if (isNotSameSection && getScrollState() == ViewPager.SCROLL_STATE_IDLE) {
                            clickTab.invoke()

                            val pagerSize = pager.size
                            if (newTabPosition != NO_POSITION) {
                                if (pagerSize == itemCount) {
                                    pager.setCurrentItem(newTabPosition, true)
                                } else {
                                    val realNewTabIndex = newTabPosition % itemTitle.size
                                    val realLastTabIndex = lastTabPosition % itemTitle.size
                                    var isIntertimalte = false
                                    if (lastTabPosition < newTabPosition) {
                                        if (isNotSameSection) {
                                            if (Math.abs(newTabPosition - lastTabPosition) != 1 && realLastTabIndex > realNewTabIndex) {
                                                isIntertimalte = true
                                            }

                                        }

                                        val newPagerIndex = when (realNewTabIndex) {
                                            0 -> viewPager!!.size - 1
                                            else -> realNewTabIndex + 1
                                        }
                                        Log.e(
                                            "Why",
                                            "${pager.currentItem}/$newPagerIndex/$newPagerIndex/$realNewTabIndex"
                                        )
                                        if (isIntertimalte) {
                                            pager.setCurrentItem( newPagerIndex - (newPagerIndex % itemTitle.size), false)
                                        }
                                        pager.currentItem = newPagerIndex
                                    } else if (lastTabPosition > newTabPosition) {
                                        if (isNotSameSection) {
                                            if (Math.abs(newTabPosition - lastTabPosition) != 1 && realLastTabIndex < realNewTabIndex) {
                                                isIntertimalte = true
                                            }

                                        }
                                        val newPagerIndex = when (realNewTabIndex) {
                                            itemSize - 1 -> 0
                                            else -> realNewTabIndex + 1
                                        }
                                        Log.e(
                                            "Why2",
                                            "${pager.currentItem}/$newPagerIndex/$newPagerIndex/$realNewTabIndex"
                                        )
                                        if (isIntertimalte) {
                                            pager.setCurrentItem( newPagerIndex % itemTitle.size + (newPagerIndex / itemTitle.size + 1), false)
                                        }
                                        pager.currentItem = newPagerIndex
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        companion object {
            private const val MAX_TAB_TEXT_LINES = 2
        }
    }

    class TabTextView(context: Context?) : AppCompatTextView(context!!) {
        fun createColorStateList(defaultColor: Int, selectedColor: Int): ColorStateList {
            val states = arrayOfNulls<IntArray>(2)
            val colors = IntArray(2)
            states[0] = SELECTED_STATE_SET
            colors[0] = selectedColor
            // Default enabled state
            states[1] = EMPTY_STATE_SET
            colors[1] = defaultColor
            return ColorStateList(states, colors)
        }
    }

    companion object {
        private const val DEFAULT_SCROLL_DURATION: Long = 200
        private const val DEFAULT_POSITION_THRESHOLD = 0.6f
        private const val POSITION_THRESHOLD_ALLOWABLE = 0.001f
    }
}
